package com.bms.subscription;

import java.time.Clock;
import java.time.LocalDate;

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

    /** A suspension outranks any subscription. */
    @Transactional(readOnly = true)
    public SubscriptionStatus status(AppUser owner) {
        if (owner.isSuspended()) {
            return SubscriptionStatus.SUSPENDED;
        }
        return periods.covers(owner.getId(), today()) ? SubscriptionStatus.ACTIVE : SubscriptionStatus.EXPIRED;
    }

    @Transactional(readOnly = true)
    public SubscriptionSummary summary(AppUser owner) {
        return new SubscriptionSummary(status(owner), periods.latestEndStartedBy(owner.getId(), today()));
    }

    LocalDate today() {
        return LocalDate.now(clock);
    }
}
