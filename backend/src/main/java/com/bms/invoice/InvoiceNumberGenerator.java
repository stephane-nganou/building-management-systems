package com.bms.invoice;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

/** Produces human readable, globally unique invoice numbers such as INV-2026-000042. */
@Component
public class InvoiceNumberGenerator {

    private final EntityManager entityManager;

    public InvoiceNumberGenerator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public String next(LocalDate issueDate) {
        Number sequence = (Number) entityManager
                .createNativeQuery("select nextval('invoice_number_seq')")
                .getSingleResult();
        return "INV-%d-%06d".formatted(issueDate.getYear(), sequence.longValue());
    }
}
