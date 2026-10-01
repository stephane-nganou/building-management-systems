package com.bms.user;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.bms.access.AssistantAssignmentRepository;
import com.bms.access.Permission;
import com.bms.subscription.SubscriptionService;
import com.bms.subscription.SubscriptionStatus;
import com.bms.user.dto.MeResponse;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentUserService currentUser;
    private final AssistantAssignmentRepository assignments;
    private final SubscriptionService subscriptions;

    public MeController(CurrentUserService currentUser, AssistantAssignmentRepository assignments,
                        SubscriptionService subscriptions) {
        this.currentUser = currentUser;
        this.assignments = assignments;
        this.subscriptions = subscriptions;
    }

    @GetMapping
    @Transactional
    public MeResponse me(Authentication authentication) {
        AppUser user = currentUser.require();
        List<MeResponse.Delegation> delegations = assignments.findByAssistantId(user.getId()).stream()
                .map(assignment -> new MeResponse.Delegation(
                        assignment.getOwner().getId(),
                        assignment.getOwner().getFullName(),
                        new TreeSet<>(assignment.getPermissions()),
                        subscriptions.status(assignment.getOwner())))
                .toList();
        boolean owner = Roles.isOwner(authentication);
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                owner,
                Roles.isAdmin(authentication),
                effectivePermissions(owner, delegations),
                user.isMustChangePassword(),
                user.isSuspended(),
                owner ? subscriptions.summary(user) : null,
                delegations);
    }

    private Set<Permission> effectivePermissions(boolean owner, List<MeResponse.Delegation> delegations) {
        if (owner) {
            return EnumSet.allOf(Permission.class);
        }
        // A suspended owner's delegation is still listed, so the assistant sees
        // why, but it grants nothing until the owner is reactivated.
        Set<Permission> granted = EnumSet.noneOf(Permission.class);
        delegations.stream()
                .filter(delegation -> delegation.ownerStatus() != SubscriptionStatus.SUSPENDED)
                .forEach(delegation -> granted.addAll(delegation.permissions()));
        return new TreeSet<>(granted);
    }
}
