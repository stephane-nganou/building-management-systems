package com.bms.subscription;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionPeriodTest {

    private final SubscriptionPeriod march = new SubscriptionPeriod(
            null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null);

    @Test
    void bothEndsAreIncluded() {
        assertThat(march.covers(LocalDate.of(2026, 3, 1))).isTrue();
        assertThat(march.covers(LocalDate.of(2026, 3, 31))).isTrue();
    }

    @Test
    void theDaysEitherSideAreNot() {
        assertThat(march.covers(LocalDate.of(2026, 2, 28))).isFalse();
        assertThat(march.covers(LocalDate.of(2026, 4, 1))).isFalse();
    }
}
