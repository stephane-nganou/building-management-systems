package com.bms.access;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bms.common.exception.AccessDeniedForResourceException;
import com.bms.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single entry point for "may the caller touch this owner's data?".
 * A user is always allowed on their own data; an assistant needs an explicit grant.
 */
@Service
public class AccessControl {

    private final AssistantAssignmentRepository assignments;
    private final CurrentUserService currentUser;

    public AccessControl(AssistantAssignmentRepository assignments, CurrentUserService currentUser) {
        this.assignments = assignments;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public UUID currentUserId() {
        return currentUser.requireId();
    }

    @Transactional(readOnly = true)
    public boolean canAccess(UUID ownerId, Permission permission) {
        UUID userId = currentUserId();
        return userId.equals(ownerId) || assignments.hasPermission(userId, ownerId, permission);
    }

    @Transactional(readOnly = true)
    public void require(UUID ownerId, Permission permission) {
        if (!canAccess(ownerId, permission)) {
            throw new AccessDeniedForResourceException("Missing permission " + permission + " for this data");
        }
    }

    /** Owner ids whose data the caller may read or write under the given permission, including their own. */
    @Transactional(readOnly = true)
    public List<UUID> accessibleOwnerIds(Permission permission) {
        UUID userId = currentUserId();
        List<UUID> ownerIds = new ArrayList<>();
        ownerIds.add(userId);
        ownerIds.addAll(assignments.findOwnerIdsGranting(userId, permission));
        return ownerIds;
    }
}
