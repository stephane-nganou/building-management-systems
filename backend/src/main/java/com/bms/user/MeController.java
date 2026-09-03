package com.bms.user;

import java.util.List;
import java.util.TreeSet;

import com.bms.access.AssistantAssignmentRepository;
import com.bms.user.dto.MeResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentUserService currentUser;
    private final AssistantAssignmentRepository assignments;

    public MeController(CurrentUserService currentUser, AssistantAssignmentRepository assignments) {
        this.currentUser = currentUser;
        this.assignments = assignments;
    }

    @GetMapping
    @Transactional
    public MeResponse me() {
        AppUser user = currentUser.require();
        List<MeResponse.Delegation> delegations = assignments.findByAssistantId(user.getId()).stream()
                .map(assignment -> new MeResponse.Delegation(
                        assignment.getOwner().getId(),
                        assignment.getOwner().getFullName(),
                        new TreeSet<>(assignment.getPermissions())))
                .toList();
        return new MeResponse(user.getId(), user.getEmail(), user.getFullName(), delegations);
    }
}
