package com.bms.tenant.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.bms.tenant.Tenant;

public record TenantResponse(
        UUID id,
        UUID apartmentId,
        String apartmentLabel,
        UUID buildingId,
        String buildingName,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate leaseStart,
        LocalDate leaseEnd,
        BigDecimal deposit,
        boolean active) {

    public static TenantResponse from(Tenant tenant) {
        var apartment = tenant.getApartment();
        return new TenantResponse(
                tenant.getId(),
                apartment.getId(),
                apartment.getLabel(),
                apartment.getBuilding().getId(),
                apartment.getBuilding().getName(),
                tenant.getFirstName(),
                tenant.getLastName(),
                tenant.getEmail(),
                tenant.getPhone(),
                tenant.getLeaseStart(),
                tenant.getLeaseEnd(),
                tenant.getDeposit(),
                tenant.isActive());
    }
}
