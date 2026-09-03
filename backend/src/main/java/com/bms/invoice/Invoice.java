package com.bms.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.bms.apartment.Apartment;
import com.bms.common.BaseEntity;
import com.bms.common.exception.ValidationException;
import com.bms.tenant.Tenant;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoice")
public class Invoice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false, updatable = false)
    private Apartment apartment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Column(name = "invoice_number", nullable = false, unique = true, updatable = false)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private InvoiceType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "notes", length = 1000)
    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt asc")
    private List<InvoiceLine> lines = new ArrayList<>();

    protected Invoice() {
        // for JPA
    }

    public Invoice(Apartment apartment, Tenant tenant, String invoiceNumber, InvoiceType type,
                   LocalDate periodStart, LocalDate periodEnd, LocalDate issueDate, LocalDate dueDate, String notes) {
        if (periodEnd.isBefore(periodStart)) {
            throw new ValidationException("Invoice period end must not be before period start");
        }
        if (dueDate.isBefore(issueDate)) {
            throw new ValidationException("Invoice due date must not be before the issue date");
        }
        this.apartment = apartment;
        this.tenant = tenant;
        this.invoiceNumber = invoiceNumber;
        this.type = type;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.notes = notes;
    }

    public void addLine(String description, BigDecimal quantity, BigDecimal unitPrice, String unit) {
        lines.add(new InvoiceLine(this, description, quantity, unitPrice, unit));
    }

    public BigDecimal getTotal() {
        return lines.stream().map(InvoiceLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Only a draft may still be edited; once sent or paid the document a tenant
     * received must stay unchanged.
     */
    public void transitionTo(InvoiceStatus target) {
        if (status == InvoiceStatus.CANCELLED) {
            throw new ValidationException("A cancelled invoice cannot change status");
        }
        if (status == InvoiceStatus.PAID && target != InvoiceStatus.CANCELLED) {
            throw new ValidationException("A paid invoice can only be cancelled");
        }
        this.status = target;
    }

    public Apartment getApartment() {
        return apartment;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public InvoiceType getType() {
        return type;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getNotes() {
        return notes;
    }

    public List<InvoiceLine> getLines() {
        return lines;
    }
}
