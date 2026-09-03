package com.bms.invoice;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdAndApartmentBuildingOwnerIdIn(UUID id, Collection<UUID> ownerIds);

    @Query("""
            select i from Invoice i
            where i.apartment.building.owner.id in :ownerIds
              and (:buildingId is null or i.apartment.building.id = :buildingId)
              and (:apartmentId is null or i.apartment.id = :apartmentId)
              and (:status is null or i.status = :status)
              and (cast(:from as date) is null or i.issueDate >= :from)
              and (cast(:to as date) is null or i.issueDate <= :to)
            order by i.issueDate desc, i.invoiceNumber desc
            """)
    List<Invoice> search(@Param("ownerIds") Collection<UUID> ownerIds,
                         @Param("buildingId") UUID buildingId,
                         @Param("apartmentId") UUID apartmentId,
                         @Param("status") InvoiceStatus status,
                         @Param("from") LocalDate from,
                         @Param("to") LocalDate to);

    /** Invoices counted as income for a reporting period, with their lines already loaded. */
    @Query("""
            select distinct i from Invoice i
            left join fetch i.lines
            where i.apartment.building.owner.id in :ownerIds
              and (:buildingId is null or i.apartment.building.id = :buildingId)
              and i.status in :statuses
              and i.issueDate between :from and :to
            """)
    List<Invoice> findForReport(@Param("ownerIds") Collection<UUID> ownerIds,
                                @Param("buildingId") UUID buildingId,
                                @Param("statuses") Collection<InvoiceStatus> statuses,
                                @Param("from") LocalDate from,
                                @Param("to") LocalDate to);
}
