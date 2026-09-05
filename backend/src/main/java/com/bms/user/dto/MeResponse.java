package com.bms.user.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bms.access.Permission;

/**
 * @param owner       whether this user manages their own buildings, as opposed
 *                    to only assisting someone else
 * @param permissions everything the caller may do anywhere: all of them for an
 *                    owner, the union of their grants for an assistant. The
 *                    frontend uses it to decide which screens exist at all.
 * @param mustChangePassword whether this account is still using a password
 *                    somebody else chose for it. While it is true the
 *                    application shows nothing but the screen that changes it.
 */
public record MeResponse(
        UUID id,
        String email,
        String name,
        boolean owner,
        Set<Permission> permissions,
        boolean mustChangePassword,
        List<Delegation> assistingFor) {

    /** An owner whose data this user may work on, and what they are allowed to do. */
    public record Delegation(UUID ownerId, String ownerName, Set<Permission> permissions) {
    }
}
