package com.bms.invoice;

import com.bms.support.AbstractIntegrationTest;
import com.bms.support.ScenarioBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.bms.support.Jwts.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvoiceAndReportIntegrationTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor OWNER = asUser("owner-inv", "owner-inv@example.com");

    @Autowired
    private ScenarioBuilder scenario;

    @Test
    void rentInvoiceLinesAreDerivedFromTheApartmentWhenNoneAreGiven() throws Exception {
        String building = scenario.createBuilding(OWNER, "Hauptstrasse 1");
        String apartment = scenario.createApartment(OWNER, building, "1A", "850.00");
        String tenant = scenario.createTenant(OWNER, apartment, "Meier");

        String invoiceId = scenario.createRentInvoice(OWNER, tenant, "2026-02-01");

        mockMvc.perform(get("/api/invoices/" + invoiceId).with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.invoiceNumber").value(org.hamcrest.Matchers.startsWith("INV-2026-")))
                .andExpect(jsonPath("$.lines.length()").value(2))
                // 850.00 base rent + 150.00 utilities advance
                .andExpect(jsonPath("$.total").value(1000.00));
    }

    @Test
    void coldWaterInvoiceWithoutLinesIsRejected() throws Exception {
        String building = scenario.createBuilding(OWNER, "Hauptstrasse 1");
        String apartment = scenario.createApartment(OWNER, building, "1A", "850.00");
        String tenant = scenario.createTenant(OWNER, apartment, "Meier");

        String body = """
                {"tenantId":"%s","type":"COLD_WATER","periodStart":"2026-02-01","periodEnd":"2026-02-28",
                 "issueDate":"2026-02-01","dueDate":"2026-02-15"}
                """.formatted(tenant);

        mockMvc.perform(post("/api/invoices").with(OWNER)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void invoiceIsRenderedAsAPdfDocument() throws Exception {
        String building = scenario.createBuilding(OWNER, "Hauptstrasse 1");
        String apartment = scenario.createApartment(OWNER, building, "1A", "850.00");
        String tenant = scenario.createTenant(OWNER, apartment, "Meier");
        String invoiceId = scenario.createRentInvoice(OWNER, tenant, "2026-02-01");

        byte[] pdf = mockMvc.perform(get("/api/invoices/" + invoiceId + "/pdf").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString(".pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void profitAndLossCountsIssuedInvoicesAndExpenses() throws Exception {
        String building = scenario.createBuilding(OWNER, "Hauptstrasse 1");
        String apartment = scenario.createApartment(OWNER, building, "1A", "850.00");
        String tenant = scenario.createTenant(OWNER, apartment, "Meier");
        String invoiceId = scenario.createRentInvoice(OWNER, tenant, "2026-02-01");
        scenario.createExpense(OWNER, building, "400.00", "2026-02-10");

        // A draft invoice is not income yet.
        mockMvc.perform(get("/api/reports/profit-loss")
                        .param("from", "2026-01-01").param("to", "2026-12-31").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpenses").value(400.00))
                .andExpect(jsonPath("$.netResult").value(-400.00));

        mockMvc.perform(post("/api/invoices/" + invoiceId + "/status").param("status", "SENT").with(OWNER))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/profit-loss")
                        .param("from", "2026-01-01").param("to", "2026-12-31").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1000.00))
                .andExpect(jsonPath("$.totalExpenses").value(400.00))
                .andExpect(jsonPath("$.netResult").value(600.00))
                .andExpect(jsonPath("$.buildings.length()").value(1))
                .andExpect(jsonPath("$.expensesByCategory[0].category").value("MAINTENANCE"));
    }

    @Test
    void profitAndLossExcludesPeriodsOutsideTheRange() throws Exception {
        String building = scenario.createBuilding(OWNER, "Hauptstrasse 1");
        scenario.createExpense(OWNER, building, "400.00", "2026-02-10");

        mockMvc.perform(get("/api/reports/profit-loss")
                        .param("from", "2026-03-01").param("to", "2026-12-31").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").value(0));
    }

    @Test
    void reportRejectsAnInvertedDateRange() throws Exception {
        mockMvc.perform(get("/api/reports/profit-loss")
                        .param("from", "2026-12-31").param("to", "2026-01-01").with(OWNER))
                .andExpect(status().isUnprocessableEntity());
    }
}
