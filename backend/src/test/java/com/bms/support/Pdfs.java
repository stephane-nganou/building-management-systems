package com.bms.support;

import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.text.PDFTextStripper;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;

/**
 * Reads a rendered PDF back: its words, since a template that resolved no
 * messages would still be a valid PDF, and its signatures, the way a PDF reader
 * checks them.
 */
public final class Pdfs {

    private Pdfs() {
    }

    public static String text(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    public static List<PDSignature> signatures(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getSignatureDictionaries();
        }
    }

    /** The signer over {@code content}, the bytes the signature's byte range covers. */
    public static SignerInformation signerOf(PDSignature signature, byte[] pdf, byte[] content) throws Exception {
        // The placeholder is zero padded past the signature, so read only its first object.
        ContentInfo info;
        try (ASN1InputStream input = new ASN1InputStream(signature.getContents(pdf))) {
            info = ContentInfo.getInstance(input.readObject());
        }
        CMSSignedData data = new CMSSignedData(new CMSProcessableByteArray(content), info);
        return data.getSignerInfos().getSigners().iterator().next();
    }

    public static SignerInformation signerOf(PDSignature signature, byte[] pdf) throws Exception {
        return signerOf(signature, pdf, signature.getSignedContent(pdf));
    }

    public static boolean verifies(SignerInformation signer, X509Certificate certificate) throws Exception {
        return signer.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certificate));
    }
}
