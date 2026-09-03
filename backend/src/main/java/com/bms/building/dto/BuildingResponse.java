package com.bms.building.dto;

import java.util.UUID;

import com.bms.building.Building;

public record BuildingResponse(
        UUID id,
        String name,
        String street,
        String city,
        String postalCode,
        String country,
        String notes,
        long apartmentCount) {

    public static BuildingResponse from(Building building, long apartmentCount) {
        var address = building.getAddress();
        return new BuildingResponse(
                building.getId(),
                building.getName(),
                address == null ? null : address.getStreet(),
                address == null ? null : address.getCity(),
                address == null ? null : address.getPostalCode(),
                address == null ? null : address.getCountry(),
                building.getNotes(),
                apartmentCount);
    }
}
