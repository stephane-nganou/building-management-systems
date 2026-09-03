package com.bms.apartment;

import java.util.List;
import java.util.UUID;

import com.bms.access.AccessControl;
import com.bms.access.Permission;
import com.bms.apartment.dto.ApartmentRequest;
import com.bms.apartment.dto.ApartmentResponse;
import com.bms.building.Building;
import com.bms.building.BuildingService;
import com.bms.common.exception.NotFoundException;
import com.bms.common.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApartmentService {

    private final ApartmentRepository apartments;
    private final BuildingService buildings;
    private final AccessControl accessControl;

    public ApartmentService(ApartmentRepository apartments, BuildingService buildings, AccessControl accessControl) {
        this.apartments = apartments;
        this.buildings = buildings;
        this.accessControl = accessControl;
    }

    @Transactional(readOnly = true)
    public List<ApartmentResponse> listAll() {
        return apartments.findByBuildingOwnerIdInOrderByLabelAsc(
                        accessControl.accessibleOwnerIds(Permission.APARTMENT_READ))
                .stream().map(ApartmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ApartmentResponse> listForBuilding(UUID buildingId) {
        buildings.require(buildingId, Permission.APARTMENT_READ);
        return apartments.findByBuildingIdOrderByLabelAsc(buildingId).stream()
                .map(ApartmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ApartmentResponse get(UUID id) {
        return ApartmentResponse.from(require(id, Permission.APARTMENT_READ));
    }

    @Transactional
    public ApartmentResponse create(UUID buildingId, ApartmentRequest request) {
        Building building = buildings.require(buildingId, Permission.APARTMENT_WRITE);
        if (apartments.existsByBuildingIdAndLabelIgnoreCase(buildingId, request.label())) {
            throw new ValidationException("Apartment '" + request.label() + "' already exists in this building");
        }
        Apartment apartment = new Apartment(building, request.label(), request.floor(), request.sizeSqm(),
                layoutOf(request), request.baseRent(), request.utilitiesAdvance(), request.status());
        return ApartmentResponse.from(apartments.save(apartment));
    }

    @Transactional
    public ApartmentResponse update(UUID id, ApartmentRequest request) {
        Apartment apartment = require(id, Permission.APARTMENT_WRITE);
        boolean labelChanged = !apartment.getLabel().equalsIgnoreCase(request.label());
        if (labelChanged
                && apartments.existsByBuildingIdAndLabelIgnoreCase(apartment.getBuilding().getId(), request.label())) {
            throw new ValidationException("Apartment '" + request.label() + "' already exists in this building");
        }
        apartment.update(request.label(), request.floor(), request.sizeSqm(), layoutOf(request),
                request.baseRent(), request.utilitiesAdvance(), request.status());
        return ApartmentResponse.from(apartment);
    }

    @Transactional
    public void delete(UUID id) {
        apartments.delete(require(id, Permission.APARTMENT_WRITE));
    }

    @Transactional(readOnly = true)
    public Apartment require(UUID id, Permission permission) {
        return apartments.findByIdAndBuildingOwnerIdIn(id, accessControl.accessibleOwnerIds(permission))
                .orElseThrow(() -> NotFoundException.of("Apartment", id));
    }

    private RoomLayout layoutOf(ApartmentRequest request) {
        return new RoomLayout(request.rooms(), request.bedrooms(), request.bathrooms(),
                request.kitchens(), request.toilets());
    }
}
