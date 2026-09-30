package com.bms.subscription.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.bms.subscription.SubscriptionPeriod;

public record PeriodResponse(UUID id, LocalDate startsOn, LocalDate endsOn, String note) {

    public static PeriodResponse from(SubscriptionPeriod period) {
        return new PeriodResponse(period.getId(), period.getStartsOn(), period.getEndsOn(), period.getNote());
    }
}
