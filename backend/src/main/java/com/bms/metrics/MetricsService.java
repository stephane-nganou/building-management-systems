package com.bms.metrics;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bms.common.exception.ValidationException;
import com.bms.invoice.InvoiceType;
import com.bms.metrics.dto.MetricsResponse;
import com.bms.metrics.dto.MetricsResponse.Activity;
import com.bms.metrics.dto.MetricsResponse.ActiveDay;
import com.bms.metrics.dto.MetricsResponse.Customers;
import com.bms.metrics.dto.MetricsResponse.DayCount;
import com.bms.metrics.dto.MetricsResponse.Endpoint;
import com.bms.metrics.dto.MetricsResponse.Features;
import com.bms.metrics.dto.MetricsResponse.Traffic;
import com.bms.metrics.dto.MetricsResponse.TrafficDay;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Puts the administrator's metrics together for the last 7, 30, 90 or 365 days, today included. */
@Service
public class MetricsService {

    static final Set<Integer> RANGES = Set.of(7, 30, 90, 365);

    /** How many endpoints each table lists, and how often one must be called to rank as slow. */
    private static final int ENDPOINTS_LISTED = 10;
    private static final int SLOW_AFTER_REQUESTS = 5;

    private final MetricsQueries queries;
    private final Clock clock;

    public MetricsService(MetricsQueries queries, Clock clock) {
        this.queries = queries;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MetricsResponse metrics(int days) {
        if (!RANGES.contains(days)) {
            throw new ValidationException("error.metrics.days");
        }
        LocalDate to = Metrics.today(clock);
        LocalDate from = to.minusDays(days - 1L);
        List<LocalDate> range = from.datesUntil(to.plusDays(1)).toList();
        OffsetDateTime since = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        return new MetricsResponse(days, from, to,
                activity(range, to),
                customers(range, since),
                features(range, since),
                traffic(range));
    }

    private Activity activity(List<LocalDate> range, LocalDate today) {
        Map<LocalDate, ActiveDay> active = byDay(queries.activeUsers(range.getFirst()), ActiveDay::day);
        return new Activity(
                filled(range, queries.signIns(range.getFirst())),
                range.stream().map(day -> active.getOrDefault(day, new ActiveDay(day, 0, 0))).toList(),
                queries.activeSince(today),
                queries.activeSince(today.minusDays(6)),
                queries.activeSince(today.minusDays(29)));
    }

    private Customers customers(List<LocalDate> range, OffsetDateTime since) {
        // Where owners stand is judged on the subscription's own today, in the
        // server's zone, so it matches the accounts screen; only the per day
        // counts use UTC days.
        LocalDate subscriptionToday = LocalDate.now(clock);
        Map<String, Long> status = queries.ownersByStatus(subscriptionToday);
        return new Customers(
                status.getOrDefault("TRIAL", 0L),
                status.getOrDefault("ACTIVE", 0L),
                status.getOrDefault("EXPIRED", 0L),
                status.getOrDefault("SUSPENDED", 0L),
                filled(range, queries.newOwners(since)),
                queries.trialsStarted(since),
                queries.trialsConverted(since));
    }

    private Features features(List<LocalDate> range, OffsetDateTime since) {
        return new Features(
                filled(range, queries.created("building", since)),
                filled(range, queries.created("apartment", since)),
                filled(range, queries.created("tenant", since)),
                filled(range, queries.created("expense", since)),
                filled(range, queries.invoices(InvoiceType.RENT, since)),
                filled(range, queries.invoices(InvoiceType.COLD_WATER, since)),
                filled(range, queries.pdfDownloads(range.getFirst())));
    }

    private Traffic traffic(List<LocalDate> range) {
        Map<LocalDate, TrafficDay> perDay = byDay(queries.traffic(range.getFirst()), TrafficDay::day);
        List<TrafficDay> days = range.stream()
                .map(day -> perDay.getOrDefault(day, new TrafficDay(day, 0, 0, 0)))
                .toList();
        List<Endpoint> endpoints = queries.endpoints(range.getFirst());
        return new Traffic(
                days.stream().mapToLong(TrafficDay::requests).sum(),
                days.stream().mapToLong(TrafficDay::clientErrors).sum(),
                days.stream().mapToLong(TrafficDay::serverErrors).sum(),
                days,
                endpoints.stream()
                        .sorted(Comparator.comparingLong(Endpoint::requests).reversed())
                        .limit(ENDPOINTS_LISTED)
                        .toList(),
                endpoints.stream()
                        .filter(endpoint -> endpoint.requests() >= SLOW_AFTER_REQUESTS)
                        .sorted(Comparator.comparingDouble(Endpoint::averageMs).reversed())
                        .limit(ENDPOINTS_LISTED)
                        .toList());
    }

    private static List<DayCount> filled(List<LocalDate> range, Map<LocalDate, Long> counts) {
        return range.stream().map(day -> new DayCount(day, counts.getOrDefault(day, 0L))).toList();
    }

    private static <T> Map<LocalDate, T> byDay(List<T> rows, Function<T, LocalDate> day) {
        return rows.stream().collect(Collectors.toMap(day, Function.identity()));
    }
}
