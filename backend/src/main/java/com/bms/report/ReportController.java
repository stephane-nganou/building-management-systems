package com.bms.report;

import java.time.LocalDate;
import java.util.UUID;

import com.bms.report.dto.DashboardSummary;
import com.bms.report.dto.ProfitLossReport;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ProfitLossService profitLoss;
    private final DashboardService dashboard;

    public ReportController(ProfitLossService profitLoss, DashboardService dashboard) {
        this.profitLoss = profitLoss;
        this.dashboard = dashboard;
    }

    @GetMapping("/profit-loss")
    public ProfitLossReport profitLoss(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID buildingId) {
        return profitLoss.report(from, to, buildingId);
    }

    @GetMapping("/summary")
    public DashboardSummary summary() {
        return dashboard.summary();
    }
}
