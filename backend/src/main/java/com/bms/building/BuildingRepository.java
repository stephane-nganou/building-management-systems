package com.bms.building;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<Building, UUID> {

    List<Building> findByOwnerIdInOrderByNameAsc(Collection<UUID> ownerIds);

    Optional<Building> findByIdAndOwnerIdIn(UUID id, Collection<UUID> ownerIds);
}
