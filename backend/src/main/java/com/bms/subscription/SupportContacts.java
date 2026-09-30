package com.bms.subscription;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** How an owner reaches customer service, about their subscription or anything else. */
@ConfigurationProperties(prefix = "bms.support")
public record SupportContacts(String email, String phone, String hours) {
}
