package com.bms.metrics;

import com.bms.metrics.dto.MetricsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administrators only; {@code SecurityConfig} refuses everyone else the whole of /api/admin. */
@RestController
@RequestMapping("/api/admin/metrics")
public class MetricsController {

    private final MetricsService metrics;

    public MetricsController(MetricsService metrics) {
        this.metrics = metrics;
    }

    /** The last {@code days} days, today included: 7, 30, 90 or 365. */
    @GetMapping
    public MetricsResponse metrics(@RequestParam(defaultValue = "30") int days) {
        return metrics.metrics(days);
    }
}
