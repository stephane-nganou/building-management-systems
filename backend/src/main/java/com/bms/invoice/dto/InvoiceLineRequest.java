package com.bms.invoice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InvoiceLineRequest(
        @NotBlank @Size(max = 500) String description,
        @NotNull @DecimalMin("0.000") BigDecimal quantity,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
        @Size(max = 255) String unit) {
}
