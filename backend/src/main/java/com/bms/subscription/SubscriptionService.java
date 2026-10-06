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

    /** The note on the period every owner starts with, which is how a trial is told from the rest. */
    public static final String TRIAL_NOTE = "Trial";

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
        periods.save(new SubscriptionPeriod(owner, today, today.plusDays(properties.trialDays() - 1L), TRIAL_NOTE));
    }

    /**
     * Takes away periods from someone the realm says is not an owner. Only the
     * V3 migration hands those out, because it had to guess who owned data.
     */
    @Transactional
    public void forget(AppUser user) {
        periods.deleteByOwner(user.getId());
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
     * began today is left with no day in it, and kept: deleting an owner's only
     * period would make them nobody's customer. Later periods stay.
     */
    @Transactional
    public void endCurrent(AppUser owner) {
        LocalDate yesterday = today().minusDays(1);
        periods.findByOwnerIdOrderByStartsOnDesc(owner.getId()).stream()
                .filter(period -> period.covers(today()))
                .forEach(period -> period.endOn(yesterday));
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
