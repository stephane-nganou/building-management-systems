package com.bms.subscription;

import java.time.LocalDate;

import com.bms.common.BaseEntity;
import com.bms.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A stretch of days, both ends included, during which an owner may change their
 * data. One ended on the day it began ends the day before it starts, and covers
 * no day at all.
 */
@Entity
@Table(name = "subscription_period")
public class SubscriptionPeriod extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private AppUser owner;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    @Column(name = "note")
    private String note;

    protected SubscriptionPeriod() {
        // for JPA
    }

    public SubscriptionPeriod(AppUser owner, LocalDate startsOn, LocalDate endsOn, String note) {
        this.owner = owner;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
        this.note = note;
    }

    public AppUser getOwner() {
        return owner;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public LocalDate getEndsOn() {
        return endsOn;
    }

    public String getNote() {
        return note;
    }

    public boolean covers(LocalDate day) {
        return !day.isBefore(startsOn) && !day.isAfter(endsOn);
    }

    public void endOn(LocalDate day) {
        this.endsOn = day;
    }
}
