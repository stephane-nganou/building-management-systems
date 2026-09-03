package com.bms.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.bms.access.AccessControl;
import com.bms.access.Permission;
import com.bms.apartment.Apartment;
import com.bms.apartment.ApartmentRepository;
import com.bms.apartment.ApartmentStatus;
import com.bms.building.BuildingRepository;
import com.bms.report.dto.DashboardSummary;
import com.bms.report.dto.ProfitLossReport;
import com.bms.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final BuildingRepository buildings;
    private final ApartmentRepository apartments;
    private final TenantRepository tenants;
    private final ProfitLossService profitLoss;
    private final AccessControl accessControl;

    public DashboardService(BuildingRepository buildings, ApartmentRepository apartments, TenantRepository tenants,
                            ProfitLossService profitLoss, AccessControl accessControl) {
        this.buildings = buildings;
        this.apartments = apartments;
        this.tenants = tenants;
        this.profitLoss = profitLoss;
        this.accessControl = accessControl;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        List<UUID> ownerIds = accessControl.accessibleOwnerIds(Permission.REPORT_READ);
        List<Apartment> allApartments = apartments.findByBuildingOwnerIdInOrderByLabelAsc(ownerIds);

        long occupied = allApartments.stream()
                .filter(apartment -> apartment.getStatus() == ApartmentStatus.OCCUPIED).count();
        BigDecimal rentRoll = allApartments.stream()
                .map(apartment -> apartment.getBaseRent().add(apartment.getUtilitiesAdvance()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();
        ProfitLossReport yearToDate = profitLoss.report(today.withDayOfYear(1), today, null);

        long activeTenants = tenants.findByApartmentBuildingOwnerIdInOrderByLastNameAsc(ownerIds).stream()
                .filter(tenant -> tenant.isActive()).count();

        return new DashboardSummary(
                buildings.findByOwnerIdInOrderByNameAsc(ownerIds).size(),
                allApartments.size(),
                occupied,
                allApartments.size() - occupied,
                activeTenants,
                rentRoll,
                yearToDate.totalIncome(),
                yearToDate.totalExpenses(),
                yearToDate.netResult());
    }
}
