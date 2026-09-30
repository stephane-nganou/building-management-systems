package com.bms.subscription;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.bms.common.exception.ValidationException;
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

    /** Everyone who has ever had a period, which is every owner and nobody else. */
    @Transactional(readOnly = true)
    public List<AppUser> owners() {
        return periods.findOwners();
    }

    @Transactional(readOnly = true)
    public boolean isOwner(UUID userId) {
        return periods.existsByOwnerId(userId);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPeriod> periods(AppUser owner) {
        return periods.findByOwnerIdOrderByStartsOnDesc(owner.getId());
    }

    @Transactional
    public SubscriptionPeriod addPeriod(AppUser owner, LocalDate startsOn, LocalDate endsOn, String note) {
        if (endsOn.isBefore(startsOn)) {
            throw new ValidationException("error.subscription.range");
        }
        return periods.save(new SubscriptionPeriod(owner, startsOn, endsOn, note));
    }

    /**
     * Makes yesterday the last day of whatever covers today. A period that only
     * began today has no day left to keep, so it goes. Later periods stay.
     */
    @Transactional
    public void endCurrent(AppUser owner) {
        LocalDate today = today();
        periods.findByOwnerIdOrderByStartsOnDesc(owner.getId()).stream()
                .filter(period -> period.covers(today))
                .forEach(period -> {
                    if (period.getStartsOn().equals(today)) {
                        periods.delete(period);
                    } else {
                        period.endOn(today.minusDays(1));
                    }
                });
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
