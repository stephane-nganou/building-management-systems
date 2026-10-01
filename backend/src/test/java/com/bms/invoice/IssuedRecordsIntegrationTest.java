package com.bms.invoice;

import java.util.UUID;

import com.bms.support.AbstractIntegrationTest;
import com.bms.support.ScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.bms.support.Jwts.asUser;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issued invoices and recorded expenses feed the profit and loss statement used
 * for tax, so deleting what they belong to must never take them along.
 */
class IssuedRecordsIntegrationTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor OWNER = asUser("owner-a", "owner-a@example.com");

    @Autowired
    private ScenarioBuilder scenario;

    @Autowired
    private JdbcTemplate jdbc;

    private String building;
    private String apartment;
    private String tenant;
    private String invoice;

    @BeforeEach
    void aTenantWithADraftInvoice() throws Exception {
        building = scenario.createBuilding(OWNER, "Hauptstrasse 1");
        apartment = scenario.createApartment(OWNER, building, "2B", "700.00");
        tenant = scenario.createTenant(OWNER, apartment, "Muster");
        invoice = scenario.createRentInvoice(OWNER, tenant, "2026-01-05");
    }

    private void issue() throws Exception {
        mockMvc.perform(post("/api/invoices/" + invoice + "/status").param("status", "SENT").with(OWNER))
                .andExpect(status().isOk());
    }

    @Test
    void aTenantWithAnIssuedInvoiceCannotBeDeleted() throws Exception {
        issue();

        mockMvc.perform(delete("/api/tenants/" + tenant).with(OWNER))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(
                        "This tenant has issued invoices, which stay in your records. Mark the tenant inactive instead."));
        mockMvc.perform(get("/api/invoices/" + invoice).with(OWNER)).andExpect(status().isOk());
    }

    @Test
    void anApartmentWithAnIssuedInvoiceCannotBeDeleted() throws Exception {
        issue();

        mockMvc.perform(delete("/api/apartments/" + apartment).with(OWNER))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/invoices/" + invoice).with(OWNER)).andExpect(status().isOk());
    }

    @Test
    void aBuildingWithAnIssuedInvoiceCannotBeDeleted() throws Exception {
        issue();

        mockMvc.perform(delete("/api/buildings/" + building).with(OWNER))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/invoices/" + invoice).with(OWNER)).andExpect(status().isOk());
    }

    /** A paid invoice, and a cancelled one, were issued too. */
    @Test
    void aCancelledInvoiceStillCountsAsIssued() throws Exception {
        issue();
        mockMvc.perform(post("/api/invoices/" + invoice + "/status").param("status", "CANCELLED").with(OWNER))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/tenants/" + tenant).with(OWNER))
                .andExpect(status().isUnprocessableEntity());
    }

    /** The chain the adversary review used: sent, back to draft, then deleted. */
    @Test
    void aSentInvoiceCannotBeWalkedBackToADraftAndDeleted() throws Exception {
        issue();

        mockMvc.perform(post("/api/invoices/" + invoice + "/status").param("status", "DRAFT").with(OWNER))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(delete("/api/invoices/" + invoice).with(OWNER))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/invoices/" + invoice).with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void aBuildingWithAnExpenseCannotBeDeleted() throws Exception {
        String other = scenario.createBuilding(OWNER, "Nebenstrasse 2");
        String expense = scenario.createExpense(OWNER, other, "120.00", "2026-01-10");

        mockMvc.perform(delete("/api/buildings/" + other).with(OWNER))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(
                        "This building has recorded expenses, which stay in your records, so it cannot be deleted."));
        mockMvc.perform(get("/api/expenses/" + expense).with(OWNER)).andExpect(status().isOk());
    }

    @Test
    void draftsGoWithTheTenant() throws Exception {
        mockMvc.perform(delete("/api/tenants/" + tenant).with(OWNER)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/invoices/" + invoice).with(OWNER)).andExpect(status().isNotFound());
    }

    @Test
    void draftsGoWithTheApartment() throws Exception {
        mockMvc.perform(delete("/api/apartments/" + apartment).with(OWNER)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/invoices/" + invoice).with(OWNER)).andExpect(status().isNotFound());
    }

    @Test
    void draftsGoWithTheBuilding() throws Exception {
        mockMvc.perform(delete("/api/buildings/" + building).with(OWNER)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/invoices/" + invoice).with(OWNER)).andExpect(status().isNotFound());
    }

    /** Whatever the code path, the database itself keeps an issued invoice. */
    @Test
    void theDatabaseRefusesToDropAnInvoiceWithItsTenant() throws Exception {
        issue();

        assertThatThrownBy(() -> jdbc.update("delete from tenant where id = ?", UUID.fromString(tenant)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
