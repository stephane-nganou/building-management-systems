package com.bms.support;

import java.time.LocalDate;
import java.util.Set;

import com.bms.access.AssistantAssignment;
import com.bms.access.AssistantAssignmentRepository;
import com.bms.access.Permission;
import com.bms.apartment.ApartmentRepository;
import com.bms.building.BuildingRepository;
import com.bms.expense.ExpenseRepository;
import com.bms.invoice.InvoiceRepository;
import com.bms.subscription.SubscriptionPeriod;
import com.bms.subscription.SubscriptionPeriodRepository;
import com.bms.tenant.TenantRepository;
import com.bms.user.AppUser;
import com.bms.user.AppUserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Lives on the test classpath only, so it is component scanned during tests alone. */
@Component
public class TestData {

    private final AppUserRepository users;
    private final AssistantAssignmentRepository assignments;
    private final BuildingRepository buildings;
    private final ApartmentRepository apartments;
    private final TenantRepository tenants;
    private final ExpenseRepository expenses;
    private final InvoiceRepository invoices;
    private final SubscriptionPeriodRepository periods;

    public TestData(AppUserRepository users, AssistantAssignmentRepository assignments,
                    BuildingRepository buildings, ApartmentRepository apartments, TenantRepository tenants,
                    ExpenseRepository expenses, InvoiceRepository invoices, SubscriptionPeriodRepository periods) {
        this.users = users;
        this.assignments = assignments;
        this.buildings = buildings;
        this.apartments = apartments;
        this.tenants = tenants;
        this.expenses = expenses;
        this.invoices = invoices;
        this.periods = periods;
    }

    @Transactional
    public void deleteAll() {
        invoices.deleteAllInBatch();
        expenses.deleteAllInBatch();
        tenants.deleteAllInBatch();
        apartments.deleteAllInBatch();
        buildings.deleteAllInBatch();
        // Bulk delete: assistant_permission rows go with it via on delete cascade.
        assignments.deleteAllInBatch();
        periods.deleteAllInBatch();
        users.deleteAllInBatch();
    }

    @Transactional
    public AppUser createUser(String keycloakId, String email, String firstName, String lastName) {
        return users.save(new AppUser(keycloakId, email, firstName, lastName));
    }

    /** Replaces the owner's subscription with one that ended yesterday. */
    public void expire(String keycloakId) {
        LocalDate today = LocalDate.now();
        setPeriod(keycloakId, today.minusDays(60), today.minusDays(1));
    }

    /** Replaces the owner's subscription with the one period given. */
    @Transactional
    public void setPeriod(String keycloakId, LocalDate startsOn, LocalDate endsOn) {
        AppUser owner = users.findByKeycloakId(keycloakId).orElseThrow();
        periods.deleteAll(periods.findByOwnerIdOrderByStartsOnDesc(owner.getId()));
        periods.save(new SubscriptionPeriod(owner, startsOn, endsOn, null));
    }

    /** Books one more period, next to whatever the owner already has. */
    @Transactional
    public void addPeriod(String keycloakId, LocalDate startsOn, LocalDate endsOn) {
        periods.save(new SubscriptionPeriod(users.findByKeycloakId(keycloakId).orElseThrow(), startsOn, endsOn, null));
    }

    @Transactional
    public void suspend(String keycloakId) {
        users.findByKeycloakId(keycloakId).orElseThrow().suspend();
    }

    @Transactional
    public AssistantAssignment assign(AppUser owner, AppUser assistant, Set<Permission> permissions) {
        return assignments.save(new AssistantAssignment(owner, assistant, permissions));
    }
}
