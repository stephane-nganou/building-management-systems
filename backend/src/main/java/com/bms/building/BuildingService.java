package com.bms.building;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bms.access.AccessControl;
import com.bms.access.Permission;
import com.bms.apartment.ApartmentRepository;
import com.bms.apartment.BuildingApartmentCount;
import com.bms.building.dto.BuildingRequest;
import com.bms.building.dto.BuildingResponse;
import com.bms.common.exception.NotFoundException;
import com.bms.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuildingService {

    private final BuildingRepository buildings;
    private final ApartmentRepository apartments;
    private final AccessControl accessControl;
    private final CurrentUserService currentUser;

    public BuildingService(BuildingRepository buildings, ApartmentRepository apartments,
                           AccessControl accessControl, CurrentUserService currentUser) {
        this.buildings = buildings;
        this.apartments = apartments;
        this.accessControl = accessControl;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<BuildingResponse> list() {
        List<Building> found = buildings.findByOwnerIdInOrderByNameAsc(
                accessControl.accessibleOwnerIds(Permission.BUILDING_READ));
        Map<UUID, Long> counts = apartmentCounts(found);
        return found.stream()
                .map(building -> BuildingResponse.from(building, counts.getOrDefault(building.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public BuildingResponse get(UUID id) {
        Building building = require(id, Permission.BUILDING_READ);
        Map<UUID, Long> counts = apartmentCounts(List.of(building));
        return BuildingResponse.from(building, counts.getOrDefault(id, 0L));
    }

    @Transactional
    public BuildingResponse create(BuildingRequest request) {
        Building building = new Building(currentUser.require(), request.name(), toAddress(request), request.notes());
        return BuildingResponse.from(buildings.save(building), 0L);
    }

    @Transactional
    public BuildingResponse update(UUID id, BuildingRequest request) {
        Building building = require(id, Permission.BUILDING_WRITE);
        building.update(request.name(), toAddress(request), request.notes());
        Map<UUID, Long> counts = apartmentCounts(List.of(building));
        return BuildingResponse.from(building, counts.getOrDefault(id, 0L));
    }

    @Transactional
    public void delete(UUID id) {
        buildings.delete(require(id, Permission.BUILDING_WRITE));
    }

    /** Loads a building the caller is allowed to touch, or fails. */
    @Transactional(readOnly = true)
    public Building require(UUID id, Permission permission) {
        return buildings.findByIdAndOwnerIdIn(id, accessControl.accessibleOwnerIds(permission))
                .orElseThrow(() -> NotFoundException.of("error.notFound.building", id));
    }

    private Map<UUID, Long> apartmentCounts(List<Building> forBuildings) {
        if (forBuildings.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = forBuildings.stream().map(Building::getId).toList();
        return apartments.countPerBuilding(ids).stream()
                .collect(Collectors.toMap(BuildingApartmentCount::buildingId, BuildingApartmentCount::count));
    }

    private Address toAddress(BuildingRequest request) {
        return new Address(request.street(), request.city(), request.postalCode(), request.country());
    }
}
