package com.bms.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.bms.expense.Expense;
import com.bms.expense.ExpenseCategory;

public record ExpenseResponse(
        UUID id,
        UUID buildingId,
        String buildingName,
        UUID apartmentId,
        String apartmentLabel,
        ExpenseCategory category,
        BigDecimal amount,
        LocalDate incurredOn,
        String description,
        String vendor) {

    public static ExpenseResponse from(Expense expense) {
        var apartment = expense.getApartment();
        return new ExpenseResponse(
                expense.getId(),
                expense.getBuilding().getId(),
                expense.getBuilding().getName(),
                apartment == null ? null : apartment.getId(),
                apartment == null ? null : apartment.getLabel(),
                expense.getCategory(),
                expense.getAmount(),
                expense.getIncurredOn(),
                expense.getDescription(),
                expense.getVendor());
    }
}
