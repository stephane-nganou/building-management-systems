package com.bms.access;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bms.access.dto.AssistantRequest;
import com.bms.access.dto.AssistantResponse;
import com.bms.access.dto.PermissionsRequest;
import com.bms.common.exception.NotFoundException;
import com.bms.common.exception.ValidationException;
import com.bms.identity.AccountService;
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
    private final AccountService accounts;

    public AssistantService(AssistantAssignmentRepository assignments, AppUserRepository users,
                            CurrentUserService currentUser, AccountService accounts) {
        this.assignments = assignments;
        this.users = users;
        this.currentUser = currentUser;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<AssistantResponse> list() {
        return assignments.findByOwnerId(currentUser.requireId()).stream()
                .map(AssistantResponse::from).toList();
    }

    /**
     * Adds an assistant. Their account is created here, with a password the
     * owner hands over, unless the email already belongs to someone.
     */
    @Transactional
    public AssistantResponse grant(AssistantRequest request) {
        AppUser owner = currentUser.require();
        return users.findByEmailIgnoreCase(request.email())
                .map(assistant -> assign(owner, assistant, request.permissions(), null))
                .orElseGet(() -> {
                    AccountService.NewAccount created = accounts.createAssistant(
                            request.email(), request.firstName(), request.lastName());
                    return assign(owner, created.user(), request.permissions(), created.password());
                });
    }

    @Transactional
    public AssistantResponse updatePermissions(UUID assignmentId, PermissionsRequest request) {
        AssistantAssignment assignment = require(assignmentId);
        assignment.replacePermissions(request.permissions());
        return AssistantResponse.from(assignment);
    }

    /** Hands the owner a new password to pass on when their assistant lost theirs. */
    @Transactional
    public AssistantResponse resetPassword(UUID assignmentId) {
        AssistantAssignment assignment = require(assignmentId);
        return AssistantResponse.from(assignment, accounts.resetPassword(assignment.getAssistant()));
    }

    @Transactional
    public void revoke(UUID assignmentId) {
        assignments.delete(require(assignmentId));
    }

    private AssistantResponse assign(AppUser owner, AppUser assistant, Set<Permission> permissions,
                                     String temporaryPassword) {
        if (assistant.getId().equals(owner.getId())) {
            throw new ValidationException("error.assistant.self");
        }
        AssistantAssignment assignment = assignments.findByOwnerIdAndAssistantId(owner.getId(), assistant.getId())
                .map(existing -> {
                    existing.replacePermissions(permissions);
                    return existing;
                })
                .orElseGet(() -> assignments.save(new AssistantAssignment(owner, assistant, permissions)));
        return AssistantResponse.from(assignment, temporaryPassword);
    }

    private AssistantAssignment require(UUID assignmentId) {
        AssistantAssignment assignment = assignments.findById(assignmentId)
                .orElseThrow(() -> NotFoundException.of("error.notFound.assistant", assignmentId));
        if (!assignment.getOwner().getId().equals(currentUser.requireId())) {
            throw NotFoundException.of("error.notFound.assistant", assignmentId);
        }
        return assignment;
    }
}
