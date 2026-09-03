package com.bms.apartment;

import java.util.UUID;

/** Number of apartments in one building, used to avoid an N+1 count per building. */
public record BuildingApartmentCount(UUID buildingId, long count) {
}
