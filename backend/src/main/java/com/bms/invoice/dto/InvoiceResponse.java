package com.bms.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.bms.invoice.Invoice;
import com.bms.invoice.InvoiceStatus;
import com.bms.invoice.InvoiceType;

public record InvoiceResponse(
        UUID id,
        String invoiceNumber,
        UUID tenantId,
        String tenantName,
        UUID apartmentId,
        String apartmentLabel,
        UUID buildingId,
        String buildingName,
        InvoiceType type,
        InvoiceStatus status,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate issueDate,
        LocalDate dueDate,
        String notes,
        BigDecimal total,
        List<Line> lines) {

    public record Line(UUID id, String description, BigDecimal quantity, BigDecimal unitPrice,
                       String unit, BigDecimal amount) {
    }

    public static InvoiceResponse from(Invoice invoice) {
        var apartment = invoice.getApartment();
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getTenant().getId(),
                invoice.getTenant().getFullName(),
                apartment.getId(),
                apartment.getLabel(),
                apartment.getBuilding().getId(),
                apartment.getBuilding().getName(),
                invoice.getType(),
                invoice.getStatus(),
                invoice.getPeriodStart(),
                invoice.getPeriodEnd(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getNotes(),
                invoice.getTotal(),
                invoice.getLines().stream()
                        .map(line -> new Line(line.getId(), line.getDescription(), line.getQuantity(),
                                line.getUnitPrice(), line.getUnit(), line.getAmount()))
                        .toList());
    }
}
