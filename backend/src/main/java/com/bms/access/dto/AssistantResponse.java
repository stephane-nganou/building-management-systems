package com.bms.access.dto;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.bms.access.AssistantAssignment;
import com.bms.access.Permission;

public record AssistantResponse(
        UUID id,
        UUID assistantId,
        String name,
        String email,
        Set<Permission> permissions) {

    public static AssistantResponse from(AssistantAssignment assignment) {
        return new AssistantResponse(
                assignment.getId(),
                assignment.getAssistant().getId(),
                assignment.getAssistant().getFullName(),
                assignment.getAssistant().getEmail(),
                new TreeSet<>(assignment.getPermissions()));
    }
}
