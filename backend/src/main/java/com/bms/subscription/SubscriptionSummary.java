package com.bms.subscription;

import java.time.LocalDate;

/**
 * @param endsOn     while active, the last day of the unbroken run of periods
 *                   from today; otherwise the last day of the latest period to
 *                   have started. Null for an owner who has never had a period
 *                   begin.
 * @param daysLeft   days after today still covered, zero on the last day; null
 *                   unless active
 * @param endingSoon whether the last day is close enough to warn the owner
 */
public record SubscriptionSummary(SubscriptionStatus status, LocalDate endsOn, Long daysLeft,
                                  boolean endingSoon) {
}
