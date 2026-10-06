package com.bms.metrics;

import java.time.Clock;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Forgets who was active once it is more than 13 months ago: long enough to
 * compare a month with the same month a year before, and no longer.
 */
@Component
public class MetricsRetention {

    static final int MONTHS_KEPT = 13;

    private final JdbcClient jdbc;
    private final Clock clock;

    public MetricsRetention(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "UTC")
    public void purge() {
        jdbc.sql("delete from user_activity where day < :cutoff")
                .param("cutoff", Metrics.today(clock).minusMonths(MONTHS_KEPT))
                .update();
    }
}
