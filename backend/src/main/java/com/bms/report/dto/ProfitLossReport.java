package com.bms.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.bms.common.CurrencyCode;
import com.bms.expense.ExpenseCategory;

/**
 * @param totals one entry per currency the buildings in scope keep their books
 *               in. Amounts in different currencies are never added together.
 */
public record ProfitLossReport(
        LocalDate from,
        LocalDate to,
        List<Totals> totals,
        List<BuildingResult> buildings,
        List<CategoryTotal> expensesByCategory) {

    public record Totals(CurrencyCode currency, BigDecimal income, BigDecimal expenses, BigDecimal netResult) {
    }

    public record BuildingResult(
            UUID buildingId,
            String buildingName,
            CurrencyCode currency,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal netResult) {
    }

    public record CategoryTotal(ExpenseCategory category, CurrencyCode currency, BigDecimal amount) {
    }
}
