package com.bms.user.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bms.access.Permission;
import com.bms.subscription.SubscriptionStatus;
import com.bms.subscription.SubscriptionSummary;

/**
 * @param owner       whether this user manages their own buildings, as opposed
 *                    to only assisting someone else
 * @param permissions everything the caller may do anywhere: all of them for an
 *                    owner, the union of their grants for an assistant. The
 *                    frontend uses it to decide which screens exist at all.
 * @param admin       whether this user administers the service's accounts. An
 *                    administrator owns no data and assists nobody.
 * @param mustChangePassword whether this account is still using a password
 *                    somebody else chose for it. While it is true the
 *                    application shows nothing but the screen that changes it.
 * @param suspended   whether an administrator has suspended this account. Every
 *                    other endpoint refuses it.
 * @param subscription an owner's own standing; null for anyone else
 */
public record MeResponse(
        UUID id,
        String email,
        String name,
        boolean owner,
        boolean admin,
        Set<Permission> permissions,
        boolean mustChangePassword,
        boolean suspended,
        SubscriptionSummary subscription,
        List<Delegation> assistingFor) {

    /**
     * An owner whose data this user may work on, what they are allowed to do,
     * and whether that owner's subscription lets anything be changed today.
     */
    public record Delegation(UUID ownerId, String ownerName, Set<Permission> permissions,
                             SubscriptionStatus ownerStatus) {
    }
}
