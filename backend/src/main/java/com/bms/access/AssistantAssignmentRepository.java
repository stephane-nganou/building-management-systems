package com.bms.access;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AssistantAssignmentRepository extends JpaRepository<AssistantAssignment, UUID> {

    List<AssistantAssignment> findByOwnerId(UUID ownerId);

    List<AssistantAssignment> findByAssistantId(UUID assistantId);

    Optional<AssistantAssignment> findByOwnerIdAndAssistantId(UUID ownerId, UUID assistantId);

    @Query("""
            select a.owner.id from AssistantAssignment a
            join a.permissions p
            where a.assistant.id = :assistantId and p = :permission
            """)
    List<UUID> findOwnerIdsGranting(UUID assistantId, Permission permission);

    @Query("""
            select count(a) > 0 from AssistantAssignment a
            join a.permissions p
            where a.assistant.id = :assistantId and a.owner.id = :ownerId and p = :permission
            """)
    boolean hasPermission(UUID assistantId, UUID ownerId, Permission permission);
}
