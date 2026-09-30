package com.bms.subscription.dto;

import java.util.List;

import com.bms.subscription.SubscriptionSummary;
import com.bms.subscription.SupportContacts;

/** Everything an owner can know about their own subscription, and whom to ask about it. */
public record SubscriptionResponse(SubscriptionSummary standing, List<PeriodResponse> periods,
                                   SupportContacts support) {
}
