package com.bms.subscription;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionPeriodRepository extends JpaRepository<SubscriptionPeriod, UUID> {

    List<SubscriptionPeriod> findByOwnerIdOrderByStartsOnDesc(UUID ownerId);

    @Query("""
            select count(p) > 0 from SubscriptionPeriod p
            where p.owner.id = :ownerId and p.startsOn <= :day and p.endsOn >= :day""")
    boolean covers(UUID ownerId, LocalDate day);

    @Query("select max(p.endsOn) from SubscriptionPeriod p where p.owner.id = :ownerId and p.startsOn <= :day")
    LocalDate latestEndStartedBy(UUID ownerId, LocalDate day);
}
