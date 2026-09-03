package com.bms.apartment;

import java.math.BigDecimal;

import com.bms.building.Building;
import com.bms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "apartment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"building_id", "label"}))
public class Apartment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false, updatable = false)
    private Building building;

    /** Apartment number or name, unique within its building. */
    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "size_sqm", precision = 8, scale = 2)
    private BigDecimal sizeSqm;

    @Embedded
    private RoomLayout rooms;

    @Column(name = "base_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseRent;

    @Column(name = "utilities_advance", nullable = false, precision = 12, scale = 2)
    private BigDecimal utilitiesAdvance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApartmentStatus status;

    protected Apartment() {
        // for JPA
    }

    public Apartment(Building building, String label, Integer floor, BigDecimal sizeSqm, RoomLayout rooms,
                     BigDecimal baseRent, BigDecimal utilitiesAdvance, ApartmentStatus status) {
        this.building = building;
        this.label = label;
        this.floor = floor;
        this.sizeSqm = sizeSqm;
        this.rooms = rooms;
        this.baseRent = baseRent;
        this.utilitiesAdvance = utilitiesAdvance;
        this.status = status;
    }

    public Building getBuilding() {
        return building;
    }

    public String getLabel() {
        return label;
    }

    public Integer getFloor() {
        return floor;
    }

    public BigDecimal getSizeSqm() {
        return sizeSqm;
    }

    public RoomLayout getRooms() {
        return rooms;
    }

    public BigDecimal getBaseRent() {
        return baseRent;
    }

    public BigDecimal getUtilitiesAdvance() {
        return utilitiesAdvance;
    }

    public ApartmentStatus getStatus() {
        return status;
    }

    public void update(String label, Integer floor, BigDecimal sizeSqm, RoomLayout rooms,
                       BigDecimal baseRent, BigDecimal utilitiesAdvance, ApartmentStatus status) {
        this.label = label;
        this.floor = floor;
        this.sizeSqm = sizeSqm;
        this.rooms = rooms;
        this.baseRent = baseRent;
        this.utilitiesAdvance = utilitiesAdvance;
        this.status = status;
    }

    public void changeStatus(ApartmentStatus status) {
        this.status = status;
    }
}
