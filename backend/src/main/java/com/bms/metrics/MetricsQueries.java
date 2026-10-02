package com.bms.metrics;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bms.invoice.InvoiceType;
import com.bms.metrics.dto.MetricsResponse.ActiveDay;
import com.bms.metrics.dto.MetricsResponse.Endpoint;
import com.bms.metrics.dto.MetricsResponse.Split;
import com.bms.metrics.dto.MetricsResponse.TrafficDay;
import com.bms.subscription.SubscriptionService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The SQL behind the metrics. Plain reporting over the tables as they are, so
 * it reads them directly rather than through entities. A day is a UTC day: a
 * timestamp is counted on the UTC date it fell on.
 *
 * <p>Per day queries return only the days that had something; the service
 * fills in the rest.
 */
@Repository
class MetricsQueries {

    private static final String UTC_DAY = "(created_at at time zone 'UTC')::date";

    private final JdbcClient jdbc;

    MetricsQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Map<LocalDate, Long> signIns(LocalDate from) {
        return perDay("""
                select day, sum(sign_ins) from user_activity
                where day >= :from and sign_ins > 0 group by day""", from);
    }

    List<ActiveDay> activeUsers(LocalDate from) {
        return jdbc.sql("""
                        select day, count(*) filter (where role = 'OWNER'),
                               count(*) filter (where role = 'ASSISTANT')
                        from user_activity where day >= :from group by day""")
                .param("from", from)
                .query((row, n) -> new ActiveDay(row.getObject(1, LocalDate.class), row.getLong(2), row.getLong(3)))
                .list();
    }

    /** Distinct owners and assistants active on or after the given day. */
    Split activeSince(LocalDate since) {
        return jdbc.sql("""
                        select count(distinct user_id) filter (where role = 'OWNER'),
                               count(distinct user_id) filter (where role = 'ASSISTANT')
                        from user_activity where day >= :since""")
                .param("since", since)
                .query((row, n) -> new Split(row.getLong(1), row.getLong(2)))
                .single();
    }

    /**
     * Owners by where they stand today, as {@code SubscriptionService.status}
     * decides it, with a trial told apart from the rest of what is active. An
     * owner is anyone who has ever had a period. When a trial and another
     * period both cover today, the other one counts: the owner has subscribed.
     */
    Map<String, Long> ownersByStatus(LocalDate today) {
        return jdbc.sql("""
                        select case when u.suspended then 'SUSPENDED'
                                    when p.id is null then 'EXPIRED'
                                    when p.note = :trial then 'TRIAL'
                                    else 'ACTIVE' end, count(*)
                        from app_user u
                        left join lateral (select id, note from subscription_period
                                           where owner_id = u.id and :today between starts_on and ends_on
                                           order by note is not distinct from :trial, ends_on desc
                                           limit 1) p on true
                        where exists (select 1 from subscription_period s where s.owner_id = u.id)
                        group by 1""")
                .param("trial", SubscriptionService.TRIAL_NOTE)
                .param("today", today)
                .query((row, n) -> Map.entry(row.getString(1), row.getLong(2)))
                .list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    Map<LocalDate, Long> newOwners(OffsetDateTime from) {
        return perDay("select " + UTC_DAY + ", count(*) from app_user"
                + " where role = 'OWNER' and created_at >= :from group by 1", from);
    }

    long trialsStarted(OffsetDateTime from) {
        return jdbc.sql("select count(*) from subscription_period where note = :trial and created_at >= :from")
                .param("trial", SubscriptionService.TRIAL_NOTE)
                .param("from", from)
                .query(Long.class)
                .single();
    }

    /**
     * Owners who had a trial and were first given another period in the range.
     * There is no payment record, so any period an administrator adds counts.
     */
    long trialsConverted(OffsetDateTime from) {
        return jdbc.sql("""
                        select count(*) from (
                            select owner_id, min(created_at) first_other from subscription_period
                            where note is distinct from :trial group by owner_id) f
                        where f.first_other >= :from
                          and exists (select 1 from subscription_period t
                                      where t.owner_id = f.owner_id and t.note = :trial)""")
                .param("trial", SubscriptionService.TRIAL_NOTE)
                .param("from", from)
                .query(Long.class)
                .single();
    }

    /** Rows created per day; {@code table} is one of those the service names, never input. */
    Map<LocalDate, Long> created(String table, OffsetDateTime from) {
        return perDay("select " + UTC_DAY + ", count(*) from " + table + " where created_at >= :from group by 1",
                from);
    }

    Map<LocalDate, Long> invoices(InvoiceType type, OffsetDateTime from) {
        return jdbc.sql("select " + UTC_DAY + ", count(*) from invoice"
                        + " where type = :type and created_at >= :from group by 1")
                .param("type", type.name())
                .param("from", from)
                .query(MetricsQueries::dayCount)
                .list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    Map<LocalDate, Long> pdfDownloads(LocalDate from) {
        return perDay("""
                select day, sum(requests) from api_stat_daily
                where day >= :from and method = 'GET' and route = '/api/invoices/{id}/pdf' and status = 200
                group by day""", from);
    }

    List<TrafficDay> traffic(LocalDate from) {
        return jdbc.sql("""
                        select day, sum(requests),
                               coalesce(sum(requests) filter (where status between 400 and 499), 0),
                               coalesce(sum(requests) filter (where status >= 500), 0)
                        from api_stat_daily where day >= :from group by day""")
                .param("from", from)
                .query((row, n) -> new TrafficDay(row.getObject(1, LocalDate.class),
                        row.getLong(2), row.getLong(3), row.getLong(4)))
                .list();
    }

    List<Endpoint> endpoints(LocalDate from) {
        return jdbc.sql("""
                        select method, route, sum(requests),
                               coalesce(sum(requests) filter (where status between 400 and 499), 0),
                               coalesce(sum(requests) filter (where status >= 500), 0),
                               sum(total_ms)::float8 / sum(requests), max(max_ms)
                        from api_stat_daily where day >= :from group by method, route""")
                .param("from", from)
                .query((row, n) -> new Endpoint(row.getString(1), row.getString(2), row.getLong(3),
                        row.getLong(4), row.getLong(5), row.getDouble(6), row.getLong(7)))
                .list();
    }

    private Map<LocalDate, Long> perDay(String sql, Object from) {
        return jdbc.sql(sql)
                .param("from", from)
                .query(MetricsQueries::dayCount)
                .list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map.Entry<LocalDate, Long> dayCount(ResultSet row, int rowNumber) throws SQLException {
        return Map.entry(row.getObject(1, LocalDate.class), row.getLong(2));
    }
}
