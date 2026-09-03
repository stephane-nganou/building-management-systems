package com.bms.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.bms.expense.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExpenseRequest(
        @NotNull UUID buildingId,
        UUID apartmentId,
        @NotNull ExpenseCategory category,
        @NotNull @DecimalMin("0.00") BigDecimal amount,
        @NotNull LocalDate incurredOn,
        @NotBlank @Size(max = 1000) String description,
        @Size(max = 255) String vendor) {
}
