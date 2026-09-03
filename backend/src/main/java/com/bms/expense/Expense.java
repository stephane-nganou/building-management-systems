package com.bms.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bms.apartment.Apartment;
import com.bms.building.Building;
import com.bms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "expense")
public class Expense extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    /** Optional: set when the expense belongs to one specific apartment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ExpenseCategory category;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "incurred_on", nullable = false)
    private LocalDate incurredOn;

    /** The reason for the expense. */
    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "vendor")
    private String vendor;

    protected Expense() {
        // for JPA
    }

    public Expense(Building building, Apartment apartment, ExpenseCategory category, BigDecimal amount,
                   LocalDate incurredOn, String description, String vendor) {
        this.building = building;
        this.apartment = apartment;
        this.category = category;
        this.amount = amount;
        this.incurredOn = incurredOn;
        this.description = description;
        this.vendor = vendor;
    }

    public Building getBuilding() {
        return building;
    }

    public Apartment getApartment() {
        return apartment;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getIncurredOn() {
        return incurredOn;
    }

    public String getDescription() {
        return description;
    }

    public String getVendor() {
        return vendor;
    }

    public void update(Apartment apartment, ExpenseCategory category, BigDecimal amount,
                       LocalDate incurredOn, String description, String vendor) {
        this.apartment = apartment;
        this.category = category;
        this.amount = amount;
        this.incurredOn = incurredOn;
        this.description = description;
        this.vendor = vendor;
    }
}
