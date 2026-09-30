package com.bms.subscription;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param trialDays   how many days, today included, a new owner may work before
 *                    anyone has paid
 * @param warningDays how many days before the last one an owner is reminded
 */
@ConfigurationProperties(prefix = "bms.subscription")
public record SubscriptionProperties(int trialDays, int warningDays) {
}
