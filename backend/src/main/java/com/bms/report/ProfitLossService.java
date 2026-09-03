package com.bms.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import com.bms.access.AccessControl;
import com.bms.access.Permission;
import com.bms.building.Building;
import com.bms.common.exception.ValidationException;
import com.bms.expense.Expense;
import com.bms.expense.ExpenseCategory;
import com.bms.expense.ExpenseRepository;
import com.bms.invoice.Invoice;
import com.bms.invoice.InvoiceRepository;
import com.bms.invoice.InvoiceStatus;
import com.bms.report.dto.ProfitLossReport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profit and loss over a period.
 *
 * <p>Income counts invoices that have actually been issued to a tenant, that is
 * {@link InvoiceStatus#SENT} and {@link InvoiceStatus#PAID}; drafts and cancelled
 * invoices are ignored. Totals are aggregated in memory so that they always match
 * the per line amounts shown on the invoice itself.
 */
@Service
public class ProfitLossService {

    private static final Set<InvoiceStatus> INCOME_STATUSES = Set.of(InvoiceStatus.SENT, InvoiceStatus.PAID);

    private final InvoiceRepository invoices;
    private final ExpenseRepository expenses;
    private final AccessControl accessControl;

    public ProfitLossService(InvoiceRepository invoices, ExpenseRepository expenses, AccessControl accessControl) {
        this.invoices = invoices;
        this.expenses = expenses;
        this.accessControl = accessControl;
    }

    @Transactional(readOnly = true)
    public ProfitLossReport report(LocalDate from, LocalDate to, UUID buildingId) {
        if (to.isBefore(from)) {
            throw new ValidationException("Report end date must not be before the start date");
        }
        List<UUID> ownerIds = accessControl.accessibleOwnerIds(Permission.REPORT_READ);

        Map<UUID, Bucket> perBuilding = new LinkedHashMap<>();
        for (Invoice invoice : invoices.findForReport(ownerIds, buildingId, INCOME_STATUSES, from, to)) {
            bucketFor(perBuilding, invoice.getApartment().getBuilding()).addIncome(invoice.getTotal());
        }

        Map<ExpenseCategory, BigDecimal> byCategory = new LinkedHashMap<>();
        for (Expense expense : expenses.search(ownerIds, buildingId, null, from, to)) {
            bucketFor(perBuilding, expense.getBuilding()).addExpense(expense.getAmount());
            byCategory.merge(expense.getCategory(), expense.getAmount(), BigDecimal::add);
        }

        List<ProfitLossReport.BuildingResult> results = perBuilding.values().stream()
                .map(Bucket::toResult)
                .sorted(Comparator.comparing(ProfitLossReport.BuildingResult::buildingName))
                .toList();

        BigDecimal totalIncome = sum(results, ProfitLossReport.BuildingResult::income);
        BigDecimal totalExpenses = sum(results, ProfitLossReport.BuildingResult::expenses);

        List<ProfitLossReport.CategoryTotal> categories = byCategory.entrySet().stream()
                .map(entry -> new ProfitLossReport.CategoryTotal(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(category -> category.category().name()))
                .toList();

        return new ProfitLossReport(from, to, totalIncome, totalExpenses,
                totalIncome.subtract(totalExpenses), results, categories);
    }

    private Bucket bucketFor(Map<UUID, Bucket> buckets, Building building) {
        return buckets.computeIfAbsent(building.getId(), id -> new Bucket(id, building.getName()));
    }

    private BigDecimal sum(List<ProfitLossReport.BuildingResult> results,
                           Function<ProfitLossReport.BuildingResult, BigDecimal> field) {
        return results.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Running income and expense totals for one building. */
    private static final class Bucket {
        private final UUID buildingId;
        private final String buildingName;
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal expenses = BigDecimal.ZERO;

        private Bucket(UUID buildingId, String buildingName) {
            this.buildingId = buildingId;
            this.buildingName = buildingName;
        }

        private void addIncome(BigDecimal amount) {
            income = income.add(amount);
        }

        private void addExpense(BigDecimal amount) {
            expenses = expenses.add(amount);
        }

        private ProfitLossReport.BuildingResult toResult() {
            return new ProfitLossReport.BuildingResult(buildingId, buildingName, income, expenses,
                    income.subtract(expenses));
        }
    }
}
