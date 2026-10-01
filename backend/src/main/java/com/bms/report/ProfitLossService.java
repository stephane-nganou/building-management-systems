package com.bms.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.bms.access.AccessControl;
import com.bms.access.Permission;
import com.bms.building.Building;
import com.bms.building.BuildingRepository;
import com.bms.common.CurrencyCode;
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
 * the per line amounts shown on the invoice itself. Each building counts in its
 * own currency, so totals are kept per currency and never added across them.
 */
@Service
public class ProfitLossService {

    private static final Set<InvoiceStatus> INCOME_STATUSES = Set.of(InvoiceStatus.SENT, InvoiceStatus.PAID);

    private final InvoiceRepository invoices;
    private final ExpenseRepository expenses;
    private final BuildingRepository buildings;
    private final AccessControl accessControl;

    public ProfitLossService(InvoiceRepository invoices, ExpenseRepository expenses, BuildingRepository buildings,
                             AccessControl accessControl) {
        this.invoices = invoices;
        this.expenses = expenses;
        this.buildings = buildings;
        this.accessControl = accessControl;
    }

    @Transactional(readOnly = true)
    public ProfitLossReport report(LocalDate from, LocalDate to, UUID buildingId) {
        if (to.isBefore(from)) {
            throw new ValidationException("error.report.dateOrder");
        }
        List<UUID> ownerIds = accessControl.accessibleOwnerIds(Permission.REPORT_READ);

        Map<UUID, Bucket> perBuilding = new LinkedHashMap<>();
        for (Invoice invoice : invoices.findForReport(ownerIds, buildingId, INCOME_STATUSES, from, to)) {
            bucketFor(perBuilding, invoice.getApartment().getBuilding()).addIncome(invoice.getTotal());
        }

        Map<CategoryInCurrency, BigDecimal> byCategory = new LinkedHashMap<>();
        for (Expense expense : expenses.search(ownerIds, buildingId, null, from, to)) {
            bucketFor(perBuilding, expense.getBuilding()).addExpense(expense.getAmount());
            byCategory.merge(new CategoryInCurrency(expense.getCategory(), expense.getBuilding().getCurrency()),
                    expense.getAmount(), BigDecimal::add);
        }

        List<ProfitLossReport.BuildingResult> results = perBuilding.values().stream()
                .map(Bucket::toResult)
                .sorted(Comparator.comparing(ProfitLossReport.BuildingResult::buildingName))
                .toList();

        List<ProfitLossReport.CategoryTotal> categories = byCategory.entrySet().stream()
                .map(entry -> new ProfitLossReport.CategoryTotal(
                        entry.getKey().category(), entry.getKey().currency(), entry.getValue()))
                .sorted(Comparator.comparing((ProfitLossReport.CategoryTotal total) -> total.category().name())
                        .thenComparing(ProfitLossReport.CategoryTotal::currency))
                .toList();

        return new ProfitLossReport(from, to, totals(ownerIds, buildingId, results), results, categories);
    }

    /**
     * One entry for every currency among the buildings in scope, so a quiet
     * period still reads as zero in the currency those buildings use.
     */
    private List<ProfitLossReport.Totals> totals(List<UUID> ownerIds, UUID buildingId,
                                                 List<ProfitLossReport.BuildingResult> results) {
        Map<CurrencyCode, ProfitLossReport.Totals> totals = new EnumMap<>(CurrencyCode.class);
        buildings.findByOwnerIdInOrderByNameAsc(ownerIds).stream()
                .filter(building -> buildingId == null || building.getId().equals(buildingId))
                .forEach(building -> totals.putIfAbsent(building.getCurrency(), zero(building.getCurrency())));
        for (ProfitLossReport.BuildingResult result : results) {
            totals.merge(result.currency(),
                    new ProfitLossReport.Totals(result.currency(), result.income(), result.expenses(),
                            result.netResult()),
                    ProfitLossService::plus);
        }
        return List.copyOf(totals.values());
    }

    private static ProfitLossReport.Totals zero(CurrencyCode currency) {
        return new ProfitLossReport.Totals(currency, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private static ProfitLossReport.Totals plus(ProfitLossReport.Totals a, ProfitLossReport.Totals b) {
        return new ProfitLossReport.Totals(a.currency(), a.income().add(b.income()),
                a.expenses().add(b.expenses()), a.netResult().add(b.netResult()));
    }

    private Bucket bucketFor(Map<UUID, Bucket> buckets, Building building) {
        return buckets.computeIfAbsent(building.getId(), id -> new Bucket(building));
    }

    private record CategoryInCurrency(ExpenseCategory category, CurrencyCode currency) {
    }

    /** Running income and expense totals for one building. */
    private static final class Bucket {
        private final UUID buildingId;
        private final String buildingName;
        private final CurrencyCode currency;
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal expenses = BigDecimal.ZERO;

        private Bucket(Building building) {
            this.buildingId = building.getId();
            this.buildingName = building.getName();
            this.currency = building.getCurrency();
        }

        private void addIncome(BigDecimal amount) {
            income = income.add(amount);
        }

        private void addExpense(BigDecimal amount) {
            expenses = expenses.add(amount);
        }

        private ProfitLossReport.BuildingResult toResult() {
            return new ProfitLossReport.BuildingResult(buildingId, buildingName, currency, income, expenses,
                    income.subtract(expenses));
        }
    }
}
