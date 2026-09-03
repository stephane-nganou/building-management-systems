package com.bms.user.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bms.access.Permission;

public record MeResponse(
        UUID id,
        String email,
        String name,
        List<Delegation> assistingFor) {

    /** An owner whose data this user may work on, and what they are allowed to do. */
    public record Delegation(UUID ownerId, String ownerName, Set<Permission> permissions) {
    }
}
