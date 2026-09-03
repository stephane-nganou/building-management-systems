package com.bms.tenant;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    List<Tenant> findByApartmentIdOrderByLastNameAsc(UUID apartmentId);

    List<Tenant> findByApartmentBuildingOwnerIdInOrderByLastNameAsc(Collection<UUID> ownerIds);

    Optional<Tenant> findByIdAndApartmentBuildingOwnerIdIn(UUID id, Collection<UUID> ownerIds);

    boolean existsByApartmentIdAndActiveTrue(UUID apartmentId);
}
