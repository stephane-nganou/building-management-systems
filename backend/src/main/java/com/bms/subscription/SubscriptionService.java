package com.bms.subscription;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.bms.common.exception.ValidationException;
import com.bms.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Comparator.comparing;

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

    /**
     * Where an owner stands today. While active, the last day is where the
     * unbroken run of periods from today ends, so a renewal already booked from
     * tomorrow means nothing is ending.
     */
    @Transactional(readOnly = true)
    public SubscriptionSummary summary(AppUser owner) {
        LocalDate today = today();
        SubscriptionStatus status = status(owner);
        if (status != SubscriptionStatus.ACTIVE) {
            return new SubscriptionSummary(status, periods.latestEndStartedBy(owner.getId(), today), null, false);
        }
        LocalDate endsOn = runsUntil(periods.findByOwnerIdOrderByStartsOnDesc(owner.getId()), today);
        long daysLeft = ChronoUnit.DAYS.between(today, endsOn);
        return new SubscriptionSummary(status, endsOn, daysLeft, daysLeft <= properties.warningDays());
    }

    /** The last day of the run of periods that covers today, joined end to start or overlapping. */
    static LocalDate runsUntil(List<SubscriptionPeriod> all, LocalDate today) {
        LocalDate end = null;
        for (SubscriptionPeriod period : all.stream().sorted(comparing(SubscriptionPeriod::getStartsOn)).toList()) {
            if (end == null && period.covers(today)) {
                end = period.getEndsOn();
            } else if (end != null && !period.getStartsOn().isAfter(end.plusDays(1))
                    && period.getEndsOn().isAfter(end)) {
                end = period.getEndsOn();
            }
        }
        return end;
    }

    LocalDate today() {
        return LocalDate.now(clock);
    }
}
