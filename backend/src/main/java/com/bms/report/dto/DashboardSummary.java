package com.bms.report.dto;

import java.math.BigDecimal;
import java.util.List;

import com.bms.common.CurrencyCode;

/**
 * @param totals one entry per currency the portfolio's buildings use; amounts
 *               in different currencies are never added together
 */
public record DashboardSummary(
        long buildingCount,
        long apartmentCount,
        long occupiedApartments,
        long vacantApartments,
        long activeTenants,
        List<Totals> totals) {

    public record Totals(
            CurrencyCode currency,
            BigDecimal monthlyRentRoll,
            BigDecimal yearToDateIncome,
            BigDecimal yearToDateExpenses,
            BigDecimal yearToDateNet) {
    }
}
