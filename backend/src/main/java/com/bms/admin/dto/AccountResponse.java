package com.bms.admin.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.bms.subscription.SubscriptionStatus;

/**
 * An owner as the administrator sees them.
 *
 * @param endsOn            the last day of the current period, or of the last one
 * @param temporaryPassword set only on the response that created the account, and
 *                          never stored. The administrator has this one chance to
 *                          pass it on.
 */
public record AccountResponse(
        UUID id,
        String email,
        String name,
        Instant createdAt,
        long buildings,
        long assistants,
        SubscriptionStatus status,
        LocalDate endsOn,
        String temporaryPassword) {
}
