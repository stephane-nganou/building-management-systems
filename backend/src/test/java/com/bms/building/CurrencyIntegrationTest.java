package com.bms.building;

import com.bms.support.AbstractIntegrationTest;
import com.bms.support.ScenarioBuilder;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.bms.support.Jwts.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Each building keeps its books in its own currency, so one owner can hold
 * buildings in different countries without their money ever being added up.
 */
class CurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor OWNER = asUser("owner-cur", "owner-cur@example.com");

    @Autowired
    private ScenarioBuilder scenario;

    @Test
    void aBuildingKeepsTheCurrencyItWasGiven() throws Exception {
        String building = scenario.createBuilding(OWNER, "Rue de la Joie", "XAF");

        mockMvc.perform(get("/api/buildings/" + building).with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("XAF"));
    }

    @Test
    void aBuildingNeedsACurrencyFromTheList() throws Exception {
        mockMvc.perform(post("/api/buildings").with(OWNER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No currency\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.currency").exists());

        mockMvc.perform(post("/api/buildings").with(OWNER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unknown currency\",\"currency\":\"JPY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void everythingInABuildingIsCountedInItsCurrency() throws Exception {
        String building = scenario.createBuilding(OWNER, "Rue de la Joie", "XAF");
        String apartment = scenario.createApartment(OWNER, building, "1A", "150000.00");
        String tenant = scenario.createTenant(OWNER, apartment, "Mbarga");
        scenario.createExpense(OWNER, building, "25000.00", "2026-02-10");

        mockMvc.perform(get("/api/apartments/" + apartment).with(OWNER))
                .andExpect(jsonPath("$.currency").value("XAF"));
        mockMvc.perform(get("/api/tenants/" + tenant).with(OWNER))
                .andExpect(jsonPath("$.currency").value("XAF"));
        mockMvc.perform(get("/api/expenses").param("buildingId", building).with(OWNER))
                .andExpect(jsonPath("$.content[0].currency").value("XAF"));
    }

    /** The CFA franc has no cents, so a line is rounded to whole francs and the total adds up on paper. */
    @Test
    void anInvoiceLineIsRoundedToTheCurrencysOwnDigits() throws Exception {
        String building = scenario.createBuilding(OWNER, "Rue de la Joie", "XAF");
        String apartment = scenario.createApartment(OWNER, building, "1A", "150000.50");
        String tenant = scenario.createTenant(OWNER, apartment, "Mbarga");
        String invoice = scenario.createRentInvoice(OWNER, tenant, "2026-02-01");

        mockMvc.perform(get("/api/invoices/" + invoice).with(OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("XAF"))
                .andExpect(jsonPath("$.lines[0].amount").value(150001))
                .andExpect(jsonPath("$.total").value(150151));

        String text = pdfText(invoice);
        assertThat(text).contains("150001 XAF").doesNotContain("EUR");
    }

    @Test
    void anInvoiceKeepsItsCurrencyWhenTheBuildingChangesItsOwn() throws Exception {
        String building = scenario.createBuilding(OWNER, "Rue de la Joie", "XAF");
        String apartment = scenario.createApartment(OWNER, building, "1A", "150000.00");
        String tenant = scenario.createTenant(OWNER, apartment, "Mbarga");
        String invoice = scenario.createRentInvoice(OWNER, tenant, "2026-02-01");

        mockMvc.perform(put("/api/buildings/" + building).with(OWNER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rue de la Joie\",\"currency\":\"XOF\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("XOF"));

        mockMvc.perform(get("/api/apartments/" + apartment).with(OWNER))
                .andExpect(jsonPath("$.currency").value("XOF"));
        mockMvc.perform(get("/api/invoices/" + invoice).with(OWNER))
                .andExpect(jsonPath("$.currency").value("XAF"));
    }

    @Test
    void reportsNeverAddOneCurrencyToAnother() throws Exception {
        RequestPostProcessor owner = asUser("owner-two-countries", "two-countries@example.com");

        String berlin = scenario.createBuilding(owner, "Berlin", "EUR");
        String berlinTenant = scenario.createTenant(owner,
                scenario.createApartment(owner, berlin, "1A", "1000.00"), "Weber");
        send(scenario.createRentInvoice(owner, berlinTenant, "2026-02-01"), owner);
        scenario.createExpense(owner, berlin, "300.00", "2026-02-10");

        String douala = scenario.createBuilding(owner, "Douala", "XAF");
        String doualaTenant = scenario.createTenant(owner,
                scenario.createApartment(owner, douala, "2B", "200000.00"), "Mbarga");
        send(scenario.createRentInvoice(owner, doualaTenant, "2026-02-01"), owner);
        scenario.createExpense(owner, douala, "50000.00", "2026-02-10");

        mockMvc.perform(get("/api/reports/profit-loss")
                        .param("from", "2026-01-01").param("to", "2026-12-31").with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.length()").value(2))
                .andExpect(jsonPath("$.totals[0].currency").value("EUR"))
                .andExpect(jsonPath("$.totals[0].income").value(1150.00))
                .andExpect(jsonPath("$.totals[0].netResult").value(850.00))
                .andExpect(jsonPath("$.totals[1].currency").value("XAF"))
                .andExpect(jsonPath("$.totals[1].income").value(200150))
                .andExpect(jsonPath("$.totals[1].netResult").value(150150.00))
                .andExpect(jsonPath("$.buildings[?(@.buildingName == 'Douala')].currency").value("XAF"))
                .andExpect(jsonPath("$.expensesByCategory.length()").value(2));

        // The dashboard's year to date follows the clock, so only its rent roll is pinned here.
        mockMvc.perform(get("/api/reports/summary").with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.length()").value(2))
                .andExpect(jsonPath("$.totals[0].currency").value("EUR"))
                .andExpect(jsonPath("$.totals[0].monthlyRentRoll").value(1150.00))
                .andExpect(jsonPath("$.totals[1].currency").value("XAF"))
                .andExpect(jsonPath("$.totals[1].monthlyRentRoll").value(200150.00));
    }

    private void send(String invoiceId, RequestPostProcessor owner) throws Exception {
        mockMvc.perform(post("/api/invoices/" + invoiceId + "/status").param("status", "SENT").with(owner))
                .andExpect(status().isOk());
    }

    private String pdfText(String invoiceId) throws Exception {
        byte[] pdf = mockMvc.perform(get("/api/invoices/" + invoiceId + "/pdf").with(OWNER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
