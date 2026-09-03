package com.bms.expense;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    Optional<Expense> findByIdAndBuildingOwnerIdIn(UUID id, Collection<UUID> ownerIds);

    @Query("""
            select e from Expense e
            where e.building.owner.id in :ownerIds
              and (:buildingId is null or e.building.id = :buildingId)
              and (:apartmentId is null or e.apartment.id = :apartmentId)
              and (cast(:from as date) is null or e.incurredOn >= :from)
              and (cast(:to as date) is null or e.incurredOn <= :to)
            order by e.incurredOn desc
            """)
    List<Expense> search(@Param("ownerIds") Collection<UUID> ownerIds,
                         @Param("buildingId") UUID buildingId,
                         @Param("apartmentId") UUID apartmentId,
                         @Param("from") LocalDate from,
                         @Param("to") LocalDate to);
}
