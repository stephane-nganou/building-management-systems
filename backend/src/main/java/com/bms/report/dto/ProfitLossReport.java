package com.bms.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.bms.expense.ExpenseCategory;

public record ProfitLossReport(
        LocalDate from,
        LocalDate to,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netResult,
        List<BuildingResult> buildings,
        List<CategoryTotal> expensesByCategory) {

    public record BuildingResult(
            UUID buildingId,
            String buildingName,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal netResult) {
    }

    public record CategoryTotal(ExpenseCategory category, BigDecimal amount) {
    }
}
