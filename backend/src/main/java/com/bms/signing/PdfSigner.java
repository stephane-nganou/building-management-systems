package com.bms.signing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.ess.ESSCertIDv2;
import org.bouncycastle.asn1.ess.SigningCertificateV2;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSAttributeTableGenerator;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Component;

/**
 * Signs a finished PDF so that a reader can tell who issued it and that it has
 * not been changed since: a PAdES baseline B signature, a detached CAdES
 * signature over the whole file but its own placeholder, appended as an
 * incremental update.
 */
@Component
public class PdfSigner {

    /** Room for a CA's chain of a few certificates, beyond PDFBox's default. */
    private static final int SIGNATURE_SIZE = SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2;

    private final SigningKey key;

    public PdfSigner(SigningKey key) {
        this.key = key;
    }

    public byte[] sign(byte[] pdf, String signer, Instant signedAt) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PDDocument document = Loader.loadPDF(withCrossReferenceTable(pdf));
             SignatureOptions options = new SignatureOptions()) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);
            signature.setName(signer);
            signature.setSignDate(calendar(signedAt));
            options.setPreferredSignatureSize(SIGNATURE_SIZE);
            document.addSignature(signature, this::cms, options);
            document.saveIncremental(output);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign the document", exception);
        }
        return output.toByteArray();
    }

    /**
     * PDFBox numbers an incremental update's objects from the highest one the
     * cross-reference lists. A cross-reference stream does not list itself, so
     * the update would reuse its number, and strict validators refuse a file
     * that redefines one. A classic table is not an object, so it cannot clash.
     */
    private static byte[] withCrossReferenceTable(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output, CompressParameters.NO_COMPRESSION);
            return output.toByteArray();
        }
    }

    /** The CMS signature over the bytes PDFBox hands us: everything but the placeholder. */
    private byte[] cms(InputStream content) {
        try {
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder().build())
                    .setSignedAttributeGenerator(signedAttributes())
                    .build(new JcaContentSignerBuilder(key.signatureAlgorithm()).build(key.privateKey()),
                            key.certificate()));
            generator.addCertificates(new JcaCertStore(key.chain()));
            return generator.generate(new CMSProcessableByteArray(content.readAllBytes()), false).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not build the signature", exception);
        }
    }

    /**
     * PAdES binds the signing certificate into the signed attributes, and takes
     * the signing time from the signature dictionary, so the CMS must not carry
     * one of its own.
     */
    private CMSAttributeTableGenerator signedAttributes() throws Exception {
        byte[] certificateHash = MessageDigest.getInstance("SHA-256").digest(key.certificate().getEncoded());
        Attribute signingCertificate = new Attribute(PKCSObjectIdentifiers.id_aa_signingCertificateV2,
                new DERSet(new SigningCertificateV2(new ESSCertIDv2(certificateHash))));
        DefaultSignedAttributeTableGenerator defaults =
                new DefaultSignedAttributeTableGenerator(new AttributeTable(new DERSet(signingCertificate)));
        return parameters -> defaults.getAttributes(parameters).remove(CMSAttributes.signingTime);
    }

    private static Calendar calendar(Instant instant) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(instant.toEpochMilli());
        return calendar;
    }
}
