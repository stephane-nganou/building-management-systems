package com.bms.apartment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApartmentRepository extends JpaRepository<Apartment, UUID> {

    @Query("""
            select new com.bms.apartment.BuildingApartmentCount(a.building.id, count(a))
            from Apartment a
            where a.building.id in :buildingIds
            group by a.building.id
            """)
    List<BuildingApartmentCount> countPerBuilding(Collection<UUID> buildingIds);

    List<Apartment> findByBuildingIdOrderByLabelAsc(UUID buildingId);

    List<Apartment> findByBuildingOwnerIdInOrderByLabelAsc(Collection<UUID> ownerIds);

    Optional<Apartment> findByIdAndBuildingOwnerIdIn(UUID id, Collection<UUID> ownerIds);

    boolean existsByBuildingIdAndLabelIgnoreCase(UUID buildingId, String label);
}
