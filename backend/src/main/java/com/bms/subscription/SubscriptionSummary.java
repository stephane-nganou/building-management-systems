package com.bms.subscription;

import java.time.LocalDate;

/**
 * @param endsOn the last day of the latest period to have started: the day the
 *               current one runs out, or the day the last one did. Null for an
 *               owner who has never had a period begin.
 */
public record SubscriptionSummary(SubscriptionStatus status, LocalDate endsOn) {
}
