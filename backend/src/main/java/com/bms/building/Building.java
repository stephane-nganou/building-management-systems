package com.bms.building;

import com.bms.common.BaseEntity;
import com.bms.common.CurrencyCode;
import com.bms.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "building")
public class Building extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private AppUser owner;

    @Column(name = "name", nullable = false)
    private String name;

    @Embedded
    private Address address;

    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * What its rent, deposits and expenses are counted in. Changing it relabels
     * them without converting anything; invoices keep the one they were issued in.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private CurrencyCode currency;

    protected Building() {
        // for JPA
    }

    public Building(AppUser owner, String name, Address address, String notes, CurrencyCode currency) {
        this.owner = owner;
        this.name = name;
        this.address = address;
        this.notes = notes;
        this.currency = currency;
    }

    public AppUser getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public Address getAddress() {
        return address;
    }

    public String getNotes() {
        return notes;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public void update(String name, Address address, String notes, CurrencyCode currency) {
        this.name = name;
        this.address = address;
        this.notes = notes;
        this.currency = currency;
    }
}
