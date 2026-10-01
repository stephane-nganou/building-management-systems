package com.bms.limits;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.bms.support.AbstractIntegrationTest;
import com.bms.support.ScenarioBuilder;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.bms.support.Jwts.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What one request may ask of the server: amounts that fit their columns, a
 * bounded number of invoice lines, a report of at most a year, and lists a page
 * at a time.
 */
class InputLimitsIntegrationTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor OWNER = asUser("owner-limits", "owner-limits@example.com");

    @Autowired
    private ScenarioBuilder scenario;

    private String building;

    @BeforeEach
    void aBuilding() throws Exception {
        building = scenario.createBuilding(OWNER, "Hauptstrasse 1");
    }

    @Test
    void anAmountTooLargeForItsColumnIsAValidationError() throws Exception {
        String body = """
                {"buildingId":"%s","category":"MAINTENANCE","amount":12345678901.00,"incurredOn":"2026-02-10",
                 "description":"Roof repair"}
                """.formatted(building);

        mockMvc.perform(post("/api/expenses").with(OWNER).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void anInvoiceWithTooManyLinesIsAValidationError() throws Exception {
        String apartment = scenario.createApartment(OWNER, building, "1A", "850.00");
        String tenant = scenario.createTenant(OWNER, apartment, "Meier");
        String lines = IntStream.range(0, 51)
                .mapToObj(i -> "{\"description\":\"Water\",\"quantity\":1,\"unitPrice\":2.50}")
                .collect(Collectors.joining(","));
        String body = """
                {"tenantId":"%s","type":"COLD_WATER","periodStart":"2026-02-01","periodEnd":"2026-02-28",
                 "issueDate":"2026-02-01","dueDate":"2026-02-15","lines":[%s]}
                """.formatted(tenant, lines);

        mockMvc.perform(post("/api/invoices").with(OWNER).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.lines").exists());
    }

    @Test
    void aReportCoversAtMostAYear() throws Exception {
        mockMvc.perform(get("/api/reports/profit-loss")
                        .param("from", "2025-01-01").param("to", "2026-01-01").with(OWNER))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("A report covers at most one year."));
    }

    @Test
    void aWholeLeapYearIsStillOneYear() throws Exception {
        mockMvc.perform(get("/api/reports/profit-loss")
                        .param("from", "2024-01-01").param("to", "2024-12-31").with(OWNER))
                .andExpect(status().isOk());
    }

    @Test
    void expensesArrivePageByPage() throws Exception {
        scenario.createExpense(OWNER, building, "100.00", "2026-02-10");
        scenario.createExpense(OWNER, building, "200.00", "2026-02-11");
        scenario.createExpense(OWNER, building, "300.00", "2026-02-12");

        mockMvc.perform(get("/api/expenses").param("size", "2").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                // Newest first, as before.
                .andExpect(jsonPath("$.content[0].amount").value(300.00))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void expensesOnTheSameDaySpreadOverPagesWithoutRepeating() throws Exception {
        Set<String> created = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            created.add(scenario.createExpense(OWNER, building, "100.00", "2026-02-10"));
        }

        Set<String> seen = new HashSet<>();
        for (int page = 0; page < 3; page++) {
            String json = mockMvc.perform(get("/api/expenses").param("size", "2").param("page", String.valueOf(page))
                            .with(OWNER))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            seen.addAll(JsonPath.read(json, "$.content[*].id"));
        }

        assertThat(seen).isEqualTo(created);
    }

    @Test
    void invoicesArrivePageByPage() throws Exception {
        String apartment = scenario.createApartment(OWNER, building, "1A", "850.00");
        String tenant = scenario.createTenant(OWNER, apartment, "Meier");
        scenario.createRentInvoice(OWNER, tenant, "2026-02-01");
        scenario.createRentInvoice(OWNER, tenant, "2026-02-10");

        mockMvc.perform(get("/api/invoices").param("size", "1").param("page", "1").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                // Newest first, so the second page holds the older one.
                .andExpect(jsonPath("$.content[0].issueDate").value("2026-02-01"))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void aPageIsNeverLargerThanTwoHundred() throws Exception {
        mockMvc.perform(get("/api/expenses").param("size", "100000").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(200));
    }

    @Test
    void aSortParameterCannotReachIntoTheQuery() throws Exception {
        scenario.createExpense(OWNER, building, "100.00", "2026-02-10");

        mockMvc.perform(get("/api/expenses").param("sort", "building.owner.keycloakId").with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }
}
