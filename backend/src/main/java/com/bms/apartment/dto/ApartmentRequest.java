package com.bms.apartment.dto;

import java.math.BigDecimal;

import com.bms.apartment.ApartmentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApartmentRequest(
        @NotBlank @Size(max = 255) String label,
        Integer floor,
        @DecimalMin("0.0") BigDecimal sizeSqm,
        @Min(0) int rooms,
        @Min(0) int bedrooms,
        @Min(0) int bathrooms,
        @Min(0) int kitchens,
        @Min(0) int toilets,
        @NotNull @DecimalMin("0.00") BigDecimal baseRent,
        @NotNull @DecimalMin("0.00") BigDecimal utilitiesAdvance,
        @NotNull ApartmentStatus status) {
}
