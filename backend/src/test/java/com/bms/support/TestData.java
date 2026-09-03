package com.bms.support;

import java.util.Set;

import com.bms.access.AssistantAssignment;
import com.bms.access.AssistantAssignmentRepository;
import com.bms.access.Permission;
import com.bms.apartment.ApartmentRepository;
import com.bms.building.BuildingRepository;
import com.bms.expense.ExpenseRepository;
import com.bms.invoice.InvoiceRepository;
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

    public TestData(AppUserRepository users, AssistantAssignmentRepository assignments,
                    BuildingRepository buildings, ApartmentRepository apartments, TenantRepository tenants,
                    ExpenseRepository expenses, InvoiceRepository invoices) {
        this.users = users;
        this.assignments = assignments;
        this.buildings = buildings;
        this.apartments = apartments;
        this.tenants = tenants;
        this.expenses = expenses;
        this.invoices = invoices;
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
        users.deleteAllInBatch();
    }

    @Transactional
    public AppUser createUser(String keycloakId, String email, String firstName, String lastName) {
        return users.save(new AppUser(keycloakId, email, firstName, lastName));
    }

    @Transactional
    public AssistantAssignment assign(AppUser owner, AppUser assistant, Set<Permission> permissions) {
        return assignments.save(new AssistantAssignment(owner, assistant, permissions));
    }
}
