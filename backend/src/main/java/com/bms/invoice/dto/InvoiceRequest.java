package com.bms.invoice.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.bms.invoice.InvoiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InvoiceRequest(
        @NotNull UUID tenantId,
        @NotNull InvoiceType type,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @NotNull LocalDate issueDate,
        @NotNull LocalDate dueDate,
        @Size(max = 1000) String notes,
        /* Optional for RENT invoices: left empty, the lines are derived from the apartment rent. */
        @Valid List<InvoiceLineRequest> lines) {
}
