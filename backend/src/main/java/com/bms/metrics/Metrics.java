package com.bms.metrics;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Metrics count by UTC day, wherever the server runs, so a day means the same
 * span of time in every table and every query.
 */
final class Metrics {

    private Metrics() {
    }

    static LocalDate today(Clock clock) {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
