package com.bms.metrics;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

import jakarta.annotation.PreDestroy;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Counts requests by route and status in memory, and adds them to the day's
 * totals once a minute and on shutdown.
 *
 * <p>Writing on every request would put a database round trip and a row lock on
 * the busiest routes into every call. A crash loses at most a minute of counts,
 * which a metric can afford. The totals are added rather than replaced, so more
 * than one instance can write them.
 */
@Component
public class RequestStats {

    record Route(String method, String route, int status) {
    }

    private record Tally(LongAdder requests, LongAdder totalMs, LongAccumulator maxMs) {
        Tally() {
            this(new LongAdder(), new LongAdder(), new LongAccumulator(Math::max, 0));
        }
    }

    private final JdbcClient jdbc;
    private final Clock clock;
    private final Map<Route, Tally> tallies = new ConcurrentHashMap<>();

    public RequestStats(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    void record(Route route, long millis) {
        Tally tally = tallies.computeIfAbsent(route, key -> new Tally());
        tally.requests().increment();
        tally.totalMs().add(millis);
        tally.maxMs().accumulate(millis);
    }

    @Scheduled(fixedDelay = 60_000)
    @PreDestroy
    public void flush() {
        tallies.forEach((route, tally) -> {
            long requests = tally.requests().sumThenReset();
            if (requests == 0) {
                return;
            }
            jdbc.sql("""
                            insert into api_stat_daily (day, method, route, status, requests, total_ms, max_ms)
                            values (:day, :method, :route, :status, :requests, :totalMs, :maxMs)
                            on conflict (day, method, route, status) do update set
                                requests = api_stat_daily.requests + excluded.requests,
                                total_ms = api_stat_daily.total_ms + excluded.total_ms,
                                max_ms = greatest(api_stat_daily.max_ms, excluded.max_ms)""")
                    .param("day", Metrics.today(clock))
                    .param("method", route.method())
                    .param("route", route.route())
                    .param("status", route.status())
                    .param("requests", requests)
                    .param("totalMs", tally.totalMs().sumThenReset())
                    .param("maxMs", tally.maxMs().getThenReset())
                    .update();
        });
    }
}
