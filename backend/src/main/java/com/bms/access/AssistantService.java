package com.bms.access;

import java.util.List;
import java.util.UUID;

import com.bms.access.dto.AssistantRequest;
import com.bms.access.dto.AssistantResponse;
import com.bms.common.exception.NotFoundException;
import com.bms.common.exception.ValidationException;
import com.bms.user.AppUser;
import com.bms.user.AppUserRepository;
import com.bms.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owners manage their own assistants; assistants cannot delegate further. */
@Service
public class AssistantService {

    private final AssistantAssignmentRepository assignments;
    private final AppUserRepository users;
    private final CurrentUserService currentUser;

    public AssistantService(AssistantAssignmentRepository assignments, AppUserRepository users,
                            CurrentUserService currentUser) {
        this.assignments = assignments;
        this.users = users;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<AssistantResponse> list() {
        return assignments.findByOwnerId(currentUser.requireId()).stream()
                .map(AssistantResponse::from).toList();
    }

    @Transactional
    public AssistantResponse grant(AssistantRequest request) {
        AppUser owner = currentUser.require();
        AppUser assistant = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ValidationException(
                        "No user with email " + request.email() + " has signed in yet"));
        if (assistant.getId().equals(owner.getId())) {
            throw new ValidationException("You cannot add yourself as an assistant");
        }
        return assignments.findByOwnerIdAndAssistantId(owner.getId(), assistant.getId())
                .map(existing -> {
                    existing.replacePermissions(request.permissions());
                    return AssistantResponse.from(existing);
                })
                .orElseGet(() -> AssistantResponse.from(
                        assignments.save(new AssistantAssignment(owner, assistant, request.permissions()))));
    }

    @Transactional
    public AssistantResponse updatePermissions(UUID assignmentId, AssistantRequest request) {
        AssistantAssignment assignment = require(assignmentId);
        assignment.replacePermissions(request.permissions());
        return AssistantResponse.from(assignment);
    }

    @Transactional
    public void revoke(UUID assignmentId) {
        assignments.delete(require(assignmentId));
    }

    private AssistantAssignment require(UUID assignmentId) {
        AssistantAssignment assignment = assignments.findById(assignmentId)
                .orElseThrow(() -> NotFoundException.of("Assistant assignment", assignmentId));
        if (!assignment.getOwner().getId().equals(currentUser.requireId())) {
            throw NotFoundException.of("Assistant assignment", assignmentId);
        }
        return assignment;
    }
}
