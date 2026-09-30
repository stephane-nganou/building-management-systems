package com.bms.admin;

import java.util.List;
import java.util.UUID;

import com.bms.access.AssistantAssignmentRepository;
import com.bms.admin.dto.AccountResponse;
import com.bms.admin.dto.OwnerRequest;
import com.bms.admin.dto.PeriodRequest;
import com.bms.building.BuildingRepository;
import com.bms.common.exception.NotFoundException;
import com.bms.identity.AccountService;
import com.bms.subscription.SubscriptionService;
import com.bms.subscription.SubscriptionStatus;
import com.bms.subscription.SubscriptionSummary;
import com.bms.subscription.dto.PeriodResponse;
import com.bms.user.AppUser;
import com.bms.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The service's customers, as their administrator manages them. Only owners are customers. */
@Service
public class AdminAccountService {

    private final AppUserRepository users;
    private final BuildingRepository buildings;
    private final AssistantAssignmentRepository assignments;
    private final SubscriptionService subscriptions;
    private final AccountService accounts;

    public AdminAccountService(AppUserRepository users, BuildingRepository buildings,
                               AssistantAssignmentRepository assignments, SubscriptionService subscriptions,
                               AccountService accounts) {
        this.users = users;
        this.buildings = buildings;
        this.assignments = assignments;
        this.subscriptions = subscriptions;
        this.accounts = accounts;
    }

    /** Every owner, or only those in the given standing. */
    @Transactional(readOnly = true)
    public List<AccountResponse> list(SubscriptionStatus status) {
        return subscriptions.owners().stream()
                .map(owner -> toResponse(owner, null))
                .filter(account -> status == null || account.status() == status)
                .toList();
    }

    @Transactional
    public AccountResponse create(OwnerRequest request) {
        AccountService.NewAccount created = accounts.createOwnerWithGeneratedPassword(
                request.email(), request.firstName(), request.lastName());
        return toResponse(created.user(), created.password());
    }

    @Transactional(readOnly = true)
    public List<PeriodResponse> periods(UUID ownerId) {
        return subscriptions.periods(requireOwner(ownerId)).stream().map(PeriodResponse::from).toList();
    }

    @Transactional
    public PeriodResponse addPeriod(UUID ownerId, PeriodRequest request) {
        return PeriodResponse.from(subscriptions.addPeriod(
                requireOwner(ownerId), request.startsOn(), request.endsOn(), request.note()));
    }

    @Transactional
    public void endCurrentPeriod(UUID ownerId) {
        subscriptions.endCurrent(requireOwner(ownerId));
    }

    @Transactional
    public AccountResponse setSuspended(UUID ownerId, boolean suspended) {
        AppUser owner = requireOwner(ownerId);
        accounts.setSuspended(owner, suspended);
        return toResponse(owner, null);
    }

    /** Assistants and administrators are not customers, so they are not found here. */
    private AppUser requireOwner(UUID id) {
        return users.findById(id)
                .filter(user -> subscriptions.isOwner(user.getId()))
                .orElseThrow(() -> NotFoundException.of("error.notFound.account", id));
    }

    private AccountResponse toResponse(AppUser owner, String temporaryPassword) {
        SubscriptionSummary summary = subscriptions.summary(owner);
        return new AccountResponse(
                owner.getId(),
                owner.getEmail(),
                owner.getFullName(),
                owner.getCreatedAt(),
                buildings.countByOwnerId(owner.getId()),
                assignments.countByOwnerId(owner.getId()),
                summary.status(),
                summary.endsOn(),
                temporaryPassword);
    }
}
