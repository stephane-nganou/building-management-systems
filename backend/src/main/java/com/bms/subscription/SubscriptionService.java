package com.bms.subscription;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import com.bms.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Decides whether an owner's subscription covers today, and opens new ones. */
@Service
public class SubscriptionService {

    private final SubscriptionPeriodRepository periods;
    private final SubscriptionProperties properties;
    private final Clock clock;

    public SubscriptionService(SubscriptionPeriodRepository periods, SubscriptionProperties properties,
                               Clock clock) {
        this.periods = periods;
        this.properties = properties;
        this.clock = clock;
    }

    /** Gives a new owner their trial, starting today. */
    @Transactional
    public void startTrial(AppUser owner) {
        LocalDate today = today();
        periods.save(new SubscriptionPeriod(owner, today, today.plusDays(properties.trialDays() - 1L), "Trial"));
    }

    @Transactional(readOnly = true)
    public boolean isActive(UUID ownerId) {
        return periods.covers(ownerId, today());
    }

    LocalDate today() {
        return LocalDate.now(clock);
    }
}
