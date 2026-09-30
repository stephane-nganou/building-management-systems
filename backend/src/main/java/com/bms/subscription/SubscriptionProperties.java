package com.bms.subscription;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** How many days, today included, a new owner may work before anyone has paid. */
@ConfigurationProperties(prefix = "bms.subscription")
public record SubscriptionProperties(int trialDays) {
}
