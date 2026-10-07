package com.bms.invoice;

import java.util.List;

import com.bms.signing.SigningKey;
import com.bms.support.AbstractIntegrationTest;
import com.bms.support.ScenarioBuilder;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.bms.support.Jwts.asUser;
import static com.bms.support.Pdfs.signatures;
import static com.bms.support.Pdfs.signerOf;
import static com.bms.support.Pdfs.text;
import static com.bms.support.Pdfs.verifies;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** An issued invoice is signed by the application and says so; a draft is neither. */
class InvoiceSignatureIntegrationTest extends AbstractIntegrationTest {

    private static final RequestPostProcessor OWNER = asUser("owner-sig", "owner-sig@example.com");

    @Autowired
    private ScenarioBuilder scenario;

    @Autowired
    private SigningKey signingKey;

    @Test
    void aDraftIsNotSigned() throws Exception {
        String invoice = newInvoice();

        byte[] pdf = pdf(invoice, "en");

        assertThat(signatures(pdf)).isEmpty();
        assertThat(text(pdf)).doesNotContain("Digitally signed");
    }

    @Test
    void aSentAndThenPaidInvoiceIsSignedByTheApplication() throws Exception {
        String invoice = newInvoice();

        moveTo(invoice, "SENT");
        assertSignedByUs(pdf(invoice, "en"));

        moveTo(invoice, "PAID");
        assertSignedByUs(pdf(invoice, "en"));
    }

    @Test
    void aCancelledInvoiceIsSigned() throws Exception {
        String invoice = newInvoice();
        moveTo(invoice, "SENT");
        moveTo(invoice, "CANCELLED");

        assertSignedByUs(pdf(invoice, "en"));
    }

    @Test
    void theSignatureLineIsWordedInEachLanguage() throws Exception {
        String invoice = newInvoice();
        moveTo(invoice, "SENT");

        assertThat(text(pdf(invoice, "en"))).contains("Digitally signed by Building Management on");
        assertThat(text(pdf(invoice, "fr"))).contains("Signée électroniquement par Building Management le");
        assertThat(text(pdf(invoice, "de"))).contains("Elektronisch signiert von Building Management am");
    }

    private void assertSignedByUs(byte[] pdf) throws Exception {
        List<PDSignature> signatures = signatures(pdf);
        assertThat(signatures).hasSize(1);
        PDSignature signature = signatures.getFirst();

        assertThat(signature.getName()).isEqualTo("Building Management");
        assertThat(verifies(signerOf(signature, pdf), signingKey.certificate())).isTrue();
        assertThat(text(pdf)).contains("Digitally signed by Building Management on");
    }

    private String newInvoice() throws Exception {
        String building = scenario.createBuilding(OWNER, "Hauptstrasse 1");
        String apartment = scenario.createApartment(OWNER, building, "1A", "850.00");
        String tenant = scenario.createTenant(OWNER, apartment, "Meier");
        return scenario.createRentInvoice(OWNER, tenant, "2026-02-01");
    }

    private void moveTo(String invoice, String status) throws Exception {
        mockMvc.perform(post("/api/invoices/" + invoice + "/status").param("status", status).with(OWNER))
                .andExpect(status().isOk());
    }

    private byte[] pdf(String invoice, String language) throws Exception {
        return mockMvc.perform(get("/api/invoices/" + invoice + "/pdf").with(OWNER)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, language))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }
}
