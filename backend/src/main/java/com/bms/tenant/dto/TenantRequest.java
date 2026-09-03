package com.bms.tenant.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TenantRequest(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @Email @Size(max = 255) String email,
        @Size(max = 255) String phone,
        @NotNull LocalDate leaseStart,
        LocalDate leaseEnd,
        @DecimalMin("0.00") BigDecimal deposit,
        boolean active) {
}
