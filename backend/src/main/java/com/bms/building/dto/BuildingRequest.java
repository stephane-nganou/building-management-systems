package com.bms.building.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuildingRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String street,
        @Size(max = 255) String city,
        @Size(max = 255) String postalCode,
        @Size(max = 255) String country,
        @Size(max = 1000) String notes) {
}
