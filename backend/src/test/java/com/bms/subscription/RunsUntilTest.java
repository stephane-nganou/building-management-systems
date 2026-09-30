package com.bms.subscription;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** How far an owner is covered from today, across periods booked back to back. */
class RunsUntilTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    private static SubscriptionPeriod period(String startsOn, String endsOn) {
        return new SubscriptionPeriod(null, LocalDate.parse(startsOn), LocalDate.parse(endsOn), null);
    }

    @Test
    void aSinglePeriodRunsToItsOwnEnd() {
        assertThat(SubscriptionService.runsUntil(List.of(period("2026-09-01", "2026-10-07")), TODAY))
                .isEqualTo("2026-10-07");
    }

    @Test
    void aRenewalStartingTheDayAfterCarriesItOn() {
        List<SubscriptionPeriod> booked = List.of(
                period("2026-10-08", "2027-10-07"),
                period("2026-09-01", "2026-10-07"));

        assertThat(SubscriptionService.runsUntil(booked, TODAY)).isEqualTo("2027-10-07");
    }

    @Test
    void anOverlappingRenewalCarriesItOnToo() {
        List<SubscriptionPeriod> booked = List.of(
                period("2026-09-01", "2026-10-07"),
                period("2026-10-05", "2026-12-31"));

        assertThat(SubscriptionService.runsUntil(booked, TODAY)).isEqualTo("2026-12-31");
    }

    @Test
    void aGapEndsTheRun() {
        List<SubscriptionPeriod> booked = List.of(
                period("2026-09-01", "2026-10-07"),
                period("2026-10-09", "2027-10-08"));

        assertThat(SubscriptionService.runsUntil(booked, TODAY)).isEqualTo("2026-10-07");
    }

    @Test
    void aShorterPeriodInsideTheRunDoesNotCutItShort() {
        List<SubscriptionPeriod> booked = List.of(
                period("2026-09-01", "2026-12-31"),
                period("2026-10-01", "2026-10-02"));

        assertThat(SubscriptionService.runsUntil(booked, TODAY)).isEqualTo("2026-12-31");
    }

    @Test
    void nothingRunsWhenNoPeriodCoversToday() {
        assertThat(SubscriptionService.runsUntil(List.of(period("2026-11-01", "2026-11-30")), TODAY)).isNull();
    }
}
