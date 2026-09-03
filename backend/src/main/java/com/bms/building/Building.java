package com.bms.building;

import com.bms.common.BaseEntity;
import com.bms.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
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

    protected Building() {
        // for JPA
    }

    public Building(AppUser owner, String name, Address address, String notes) {
        this.owner = owner;
        this.name = name;
        this.address = address;
        this.notes = notes;
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

    public void update(String name, Address address, String notes) {
        this.name = name;
        this.address = address;
        this.notes = notes;
    }
}
