package com.bms.access.dto;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.bms.access.AssistantAssignment;
import com.bms.access.Permission;

/**
 * @param temporaryPassword set only on the response that created the account or
 *                          reset its password, and never stored. The owner has
 *                          this one chance to pass it on.
 */
public record AssistantResponse(
        UUID id,
        UUID assistantId,
        String name,
        String email,
        Set<Permission> permissions,
        String temporaryPassword) {

    public static AssistantResponse from(AssistantAssignment assignment) {
        return from(assignment, null);
    }

    public static AssistantResponse from(AssistantAssignment assignment, String temporaryPassword) {
        return new AssistantResponse(
                assignment.getId(),
                assignment.getAssistant().getId(),
                assignment.getAssistant().getFullName(),
                assignment.getAssistant().getEmail(),
                new TreeSet<>(assignment.getPermissions()),
                temporaryPassword);
    }
}
