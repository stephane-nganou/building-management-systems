package com.bms.user;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.bms.access.AssistantAssignmentRepository;
import com.bms.access.Permission;
import com.bms.user.dto.MeResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private static final String OWNER_AUTHORITY = "ROLE_OWNER";
    private static final String ASSISTANT_AUTHORITY = "ROLE_ASSISTANT";

    private final CurrentUserService currentUser;
    private final AssistantAssignmentRepository assignments;

    public MeController(CurrentUserService currentUser, AssistantAssignmentRepository assignments) {
        this.currentUser = currentUser;
        this.assignments = assignments;
    }

    @GetMapping
    @Transactional
    public MeResponse me(Authentication authentication) {
        AppUser user = currentUser.require();
        List<MeResponse.Delegation> delegations = assignments.findByAssistantId(user.getId()).stream()
                .map(assignment -> new MeResponse.Delegation(
                        assignment.getOwner().getId(),
                        assignment.getOwner().getFullName(),
                        new TreeSet<>(assignment.getPermissions())))
                .toList();
        boolean owner = isOwner(authentication);
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                owner,
                effectivePermissions(owner, delegations),
                user.isMustChangePassword(),
                delegations);
    }

    /**
     * Someone is an assistant only when the realm says so and says nothing about
     * owning. Anything else is treated as an owner, which is what a user who
     * signed up before roles existed still is.
     */
    private boolean isOwner(Authentication authentication) {
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return authorities.contains(OWNER_AUTHORITY) || !authorities.contains(ASSISTANT_AUTHORITY);
    }

    private Set<Permission> effectivePermissions(boolean owner, List<MeResponse.Delegation> delegations) {
        if (owner) {
            return EnumSet.allOf(Permission.class);
        }
        Set<Permission> granted = EnumSet.noneOf(Permission.class);
        delegations.forEach(delegation -> granted.addAll(delegation.permissions()));
        return new TreeSet<>(granted);
    }
}
