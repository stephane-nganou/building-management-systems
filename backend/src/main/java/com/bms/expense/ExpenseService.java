package com.bms.expense;

import java.time.LocalDate;
import java.util.UUID;

import com.bms.access.AccessControl;
import com.bms.access.Permission;
import com.bms.apartment.Apartment;
import com.bms.apartment.ApartmentService;
import com.bms.building.Building;
import com.bms.building.BuildingService;
import com.bms.common.exception.NotFoundException;
import com.bms.common.exception.ValidationException;
import com.bms.expense.dto.ExpenseRequest;
import com.bms.expense.dto.ExpenseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final ExpenseRepository expenses;
    private final BuildingService buildings;
    private final ApartmentService apartments;
    private final AccessControl accessControl;

    public ExpenseService(ExpenseRepository expenses, BuildingService buildings, ApartmentService apartments,
                          AccessControl accessControl) {
        this.expenses = expenses;
        this.buildings = buildings;
        this.apartments = apartments;
        this.accessControl = accessControl;
    }

    /** Newest first, always: a caller picks the page, never the order. */
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> search(UUID buildingId, UUID apartmentId, LocalDate from, LocalDate to,
                                        Pageable page) {
        return expenses.search(accessControl.accessibleOwnerIds(Permission.EXPENSE_READ),
                        buildingId, apartmentId, from, to, PageRequest.of(page.getPageNumber(), page.getPageSize()))
                .map(ExpenseResponse::from);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(UUID id) {
        return ExpenseResponse.from(require(id, Permission.EXPENSE_READ));
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Building building = buildings.require(request.buildingId(), Permission.EXPENSE_WRITE);
        Apartment apartment = resolveApartment(request, building);
        Expense expense = new Expense(building, apartment, request.category(), request.amount(),
                request.incurredOn(), request.description(), request.vendor());
        return ExpenseResponse.from(expenses.save(expense));
    }

    @Transactional
    public ExpenseResponse update(UUID id, ExpenseRequest request) {
        Expense expense = require(id, Permission.EXPENSE_WRITE);
        Apartment apartment = resolveApartment(request, expense.getBuilding());
        expense.update(apartment, request.category(), request.amount(), request.incurredOn(),
                request.description(), request.vendor());
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void delete(UUID id) {
        expenses.delete(require(id, Permission.EXPENSE_WRITE));
    }

    private Expense require(UUID id, Permission permission) {
        Expense expense = expenses.findByIdAndBuildingOwnerIdIn(id, accessControl.accessibleOwnerIds(permission))
                .orElseThrow(() -> NotFoundException.of("error.notFound.expense", id));
        accessControl.requireWritable(permission, expense.getBuilding().getOwner());
        return expense;
    }

    /** An apartment level expense must belong to the building the expense is booked against. */
    private Apartment resolveApartment(ExpenseRequest request, Building building) {
        if (request.apartmentId() == null) {
            return null;
        }
        Apartment apartment = apartments.require(request.apartmentId(), Permission.EXPENSE_WRITE);
        if (!apartment.getBuilding().getId().equals(building.getId())) {
            throw new ValidationException("error.expense.apartmentNotInBuilding");
        }
        return apartment;
    }
}
