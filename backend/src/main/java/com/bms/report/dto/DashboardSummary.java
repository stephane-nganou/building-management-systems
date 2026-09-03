package com.bms.report.dto;

import java.math.BigDecimal;

public record DashboardSummary(
        long buildingCount,
        long apartmentCount,
        long occupiedApartments,
        long vacantApartments,
        long activeTenants,
        BigDecimal monthlyRentRoll,
        BigDecimal yearToDateIncome,
        BigDecimal yearToDateExpenses,
        BigDecimal yearToDateNet) {
}
