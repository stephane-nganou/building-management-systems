package com.bms.i18n;

import java.util.UUID;

import com.bms.support.AbstractIntegrationTest;
import com.bms.support.ScenarioBuilder;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.bms.support.Jwts.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The API answers in the language the caller asked for. The frontend sends
 * `Accept-Language` on every request, so this is the whole of what it needs.
 */
class LanguageIntegrationTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor OWNER = asUser("owner-lang", "owner-lang@example.com");

    @Autowired
    private ScenarioBuilder scenario;

    @Test
    void anErrorIsWordedInEnglishWhenNoLanguageIsAskedFor() throws Exception {
        UUID missing = UUID.randomUUID();

        mockMvc.perform(get("/api/buildings/" + missing).with(OWNER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Building " + missing + " was not found"));
    }

    @Test
    void anErrorIsWordedInFrenchWhenTheCallerAsksForIt() throws Exception {
        UUID missing = UUID.randomUUID();

        mockMvc.perform(get("/api/buildings/" + missing).with(OWNER)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "fr"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("L’immeuble " + missing + " est introuvable"));
    }

    /** A language we do not speak falls back to English rather than to a half translated page. */
    @Test
    void anUnsupportedLanguageFallsBackToEnglish() throws Exception {
        UUID missing = UUID.randomUUID();

        mockMvc.perform(get("/api/buildings/" + missing).with(OWNER)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "de-DE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Building " + missing + " was not found"));
    }

    @Test
    void aBusinessRuleIsWordedInFrenchToo() throws Exception {
        String building = scenario.createBuilding(OWNER, "Hauptstrasse 1");
        String apartment = scenario.createApartment(OWNER, building, "1A", "850.00");
        String tenant = scenario.createTenant(OWNER, apartment, "Meier");

        String body = """
                {"tenantId":"%s","type":"COLD_WATER","periodStart":"2026-02-01","periodEnd":"2026-02-28",
                 "issueDate":"2026-02-01","dueDate":"2026-02-15"}
                """.formatted(tenant);

        mockMvc.perform(post("/api/invoices").with(OWNER).header(HttpHeaders.ACCEPT_LANGUAGE, "fr")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Une facture Eau froide demande au moins une ligne"));
    }

    /**
     * Rent lines are generated once, when the invoice is created, and stored.
     * They stay in the language they were written in, exactly like a line the
     * user typed; only the wording around them follows the download request.
     */
    @Test
    void generatedRentLinesAreWrittenInTheLanguageTheInvoiceWasCreatedIn() throws Exception {
        String building = scenario.createBuilding(OWNER, "Rue Principale");
        String apartment = scenario.createApartment(OWNER, building, "2B", "700.00");
        String tenant = scenario.createTenant(OWNER, apartment, "Dupont");

        String body = """
                {"tenantId":"%s","type":"RENT","periodStart":"2026-03-01","periodEnd":"2026-03-31",
                 "issueDate":"2026-03-01","dueDate":"2026-03-15"}
                """.formatted(tenant);
        String created = mockMvc.perform(post("/api/invoices").with(OWNER)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "fr")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lines[0].description").value("Loyer 2B"))
                .andExpect(jsonPath("$.lines[0].unit").value("mois"))
                .andExpect(jsonPath("$.lines[1].description").value("Provision pour charges"))
                .andReturn().getResponse().getContentAsString();

        String invoiceId = com.jayway.jsonpath.JsonPath.read(created, "$.id");

        // The headings are upper cased by the stylesheet, so they come back out
        // of the document that way.
        assertThat(pdfText(invoiceId, "fr"))
                .contains("FACTURÉ À", "PÉRIODE FACTURÉE", "Total à payer", "Loyer 2B")
                .contains("Merci de virer le total dû")
                .doesNotContain("Total due");

        // The same invoice downloaded in English: the wording around the lines
        // follows the request, the stored lines do not.
        assertThat(pdfText(invoiceId, "en"))
                .contains("BILLED TO", "BILLING PERIOD", "Total due", "Loyer 2B")
                .contains("Please transfer the total due")
                .doesNotContain("Total à payer");
    }

    private String pdfText(String invoiceId, String language) throws Exception {
        byte[] pdf = mockMvc.perform(get("/api/invoices/" + invoiceId + "/pdf").with(OWNER)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, language))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
