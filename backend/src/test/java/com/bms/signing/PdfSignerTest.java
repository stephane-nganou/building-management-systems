package com.bms.signing;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.CMSSignerDigestMismatchException;
import org.bouncycastle.cms.SignerInformation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.bms.support.Pdfs.signatures;
import static com.bms.support.Pdfs.signerOf;
import static com.bms.support.Pdfs.verifies;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A signed PDF names its signer, verifies against our certificate, and stops verifying once changed. */
class PdfSignerTest {

    private static final Instant SIGNED_AT = Instant.parse("2026-10-06T09:30:00Z");

    private static SigningKey key;
    private static byte[] signed;
    private static PDSignature signature;

    @BeforeAll
    static void signABlankPage() throws Exception {
        key = SigningKey.selfSigned("Building Management", SIGNED_AT);
        signed = new PdfSigner(key).sign(blankPdf(), "Building Management", SIGNED_AT);
        List<PDSignature> all = signatures(signed);
        assertThat(all).hasSize(1);
        signature = all.getFirst();
    }

    @Test
    void theSignatureIsPadesAndNamesTheIssuer() {
        assertThat(signature.getSubFilter()).isEqualTo(PDSignature.SUBFILTER_ETSI_CADES_DETACHED.getName());
        assertThat(signature.getName()).isEqualTo("Building Management");
        assertThat(signature.getSignDate().toInstant()).isEqualTo(SIGNED_AT);
    }

    @Test
    void theSignatureVerifiesAgainstOurCertificate() throws Exception {
        SignerInformation signer = signerOf(signature, signed);

        assertThat(verifies(signer, key.certificate())).isTrue();
        assertThat(signer.getSignedAttributes().get(PKCSObjectIdentifiers.id_aa_signingCertificateV2)).isNotNull();
        assertThat(signer.getSignedAttributes().get(CMSAttributes.signingTime)).isNull();
    }

    @Test
    void aChangedDocumentNoLongerVerifies() throws Exception {
        byte[] content = signature.getSignedContent(signed);
        content[content.length / 2] ^= 1;

        SignerInformation signer = signerOf(signature, signed, content);

        assertThatThrownBy(() -> verifies(signer, key.certificate()))
                .isInstanceOf(CMSSignerDigestMismatchException.class);
    }

    /**
     * PDFBox numbers an update's objects from the highest one a cross-reference
     * lists, and a cross-reference stream does not list itself: signing such a
     * file reused the stream's number, which strict validators refuse to read.
     */
    @Test
    void bothRevisionsEndInACrossReferenceTable() {
        String text = new String(signed, StandardCharsets.ISO_8859_1);

        assertThat(Pattern.compile("(?m)^startxref$").matcher(text).results()).hasSize(2);
        assertThat(Pattern.compile("(?m)^xref$").matcher(text).results()).hasSize(2);
    }

    /** Saved as PDFBox and openhtmltopdf save by default: with a cross-reference stream. */
    private static byte[] blankPdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }
}
