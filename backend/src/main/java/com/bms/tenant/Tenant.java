package com.bms.tenant;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bms.apartment.Apartment;
import com.bms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant")
public class Tenant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "lease_start", nullable = false)
    private LocalDate leaseStart;

    @Column(name = "lease_end")
    private LocalDate leaseEnd;

    @Column(name = "deposit", precision = 12, scale = 2)
    private BigDecimal deposit;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Tenant() {
        // for JPA
    }

    public Tenant(Apartment apartment, String firstName, String lastName, String email, String phone,
                  LocalDate leaseStart, LocalDate leaseEnd, BigDecimal deposit) {
        this.apartment = apartment;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.leaseStart = leaseStart;
        this.leaseEnd = leaseEnd;
        this.deposit = deposit;
    }

    public Apartment getApartment() {
        return apartment;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getLeaseStart() {
        return leaseStart;
    }

    public LocalDate getLeaseEnd() {
        return leaseEnd;
    }

    public BigDecimal getDeposit() {
        return deposit;
    }

    public boolean isActive() {
        return active;
    }

    public void update(String firstName, String lastName, String email, String phone,
                       LocalDate leaseStart, LocalDate leaseEnd, BigDecimal deposit, boolean active) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.leaseStart = leaseStart;
        this.leaseEnd = leaseEnd;
        this.deposit = deposit;
        this.active = active;
    }

    public void moveTo(Apartment apartment) {
        this.apartment = apartment;
    }
}
