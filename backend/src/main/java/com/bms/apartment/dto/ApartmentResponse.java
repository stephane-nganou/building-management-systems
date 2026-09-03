package com.bms.apartment.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.bms.apartment.Apartment;
import com.bms.apartment.ApartmentStatus;

public record ApartmentResponse(
        UUID id,
        UUID buildingId,
        String buildingName,
        String label,
        Integer floor,
        BigDecimal sizeSqm,
        int rooms,
        int bedrooms,
        int bathrooms,
        int kitchens,
        int toilets,
        BigDecimal baseRent,
        BigDecimal utilitiesAdvance,
        ApartmentStatus status) {

    public static ApartmentResponse from(Apartment apartment) {
        var rooms = apartment.getRooms();
        return new ApartmentResponse(
                apartment.getId(),
                apartment.getBuilding().getId(),
                apartment.getBuilding().getName(),
                apartment.getLabel(),
                apartment.getFloor(),
                apartment.getSizeSqm(),
                rooms.getRooms(),
                rooms.getBedrooms(),
                rooms.getBathrooms(),
                rooms.getKitchens(),
                rooms.getToilets(),
                apartment.getBaseRent(),
                apartment.getUtilitiesAdvance(),
                apartment.getStatus());
    }
}
