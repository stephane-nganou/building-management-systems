package com.bms.invoice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.bms.common.i18n.Messages;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PDFontSupplier;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** Renders an invoice as styled HTML, then converts that HTML to a PDF document. */
@Component
public class InvoicePdfRenderer {

    /** A static cut of the app's typeface; the PDF renderer cannot read variable fonts. */
    private record Face(String file, String family, int weight) {
    }

    private static final List<Face> FACES = List.of(
            new Face("MonaSans-Regular.ttf", "Mona Sans", 400),
            new Face("MonaSans-SemiBold.ttf", "Mona Sans", 600),
            new Face("MonaSansExpanded-Bold.ttf", "Mona Sans Expanded", 700));

    private final TemplateEngine templateEngine;
    private final Messages messages;
    private final String issuerName;

    public InvoicePdfRenderer(TemplateEngine templateEngine, Messages messages,
                              @Value("${bms.invoice.issuer-name}") String issuerName) {
        this.templateEngine = templateEngine;
        this.messages = messages;
        this.issuerName = issuerName;
    }

    /**
     * The locale words the document. Line descriptions are not translated here:
     * they were stored when the invoice was created, whether we generated them
     * or the user typed them.
     */
    public byte[] render(Invoice invoice, Locale locale) {
        Context context = new Context(locale);
        context.setVariable("invoice", invoice);
        context.setVariable("building", invoice.getApartment().getBuilding());
        context.setVariable("owner", invoice.getApartment().getBuilding().getOwner());
        context.setVariable("issuerName", issuerName);
        // The invoice's own currency, which its building may since have changed.
        context.setVariable("currency", invoice.getCurrency().name());
        context.setVariable("digits", invoice.getCurrency().fractionDigits());
        // Each language writes the day first, but not with the same separator.
        DateTimeFormatter date = DateTimeFormatter.ofPattern(messages.get(locale, "invoice.datePattern"));
        context.setVariable("issueDate", date.format(invoice.getIssueDate()));
        context.setVariable("dueDate", date.format(invoice.getDueDate()));
        context.setVariable("periodStart", date.format(invoice.getPeriodStart()));
        context.setVariable("periodEnd", date.format(invoice.getPeriodEnd()));

        String html = templateEngine.process("invoice", context);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.usePDDocument(document);
            for (Face face : FACES) {
                builder.useFont(new PDFontSupplier(load(document, face.file())), face.family(), face.weight(),
                        FontStyle.NORMAL, true);
            }
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not render invoice " + invoice.getInvoiceNumber(), exception);
        }
        return output.toByteArray();
    }

    /**
     * Mona Sans joins "ti", "tt", "ff" and a few more into ligatures, and PDFBox
     * applies them as it writes. Those glyphs have no character behind them, so
     * text copied or searched out of the PDF would lose its letters. Without the
     * substitution table every glyph maps back to the character it draws.
     */
    private static PDFont load(PDDocument document, String file) throws IOException {
        try (InputStream stream = InvoicePdfRenderer.class.getResourceAsStream("/fonts/" + file)) {
            TrueTypeFont font = new TTFParser().parse(new RandomAccessReadBuffer(stream));
            font.getTableMap().remove("GSUB");
            return PDType0Font.load(document, font, true);
        }
    }
}
