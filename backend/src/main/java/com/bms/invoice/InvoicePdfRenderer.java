package com.bms.invoice;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** Renders an invoice as styled HTML, then converts that HTML to a PDF document. */
@Component
public class InvoicePdfRenderer {

    /** Day before month, as both languages the app speaks write it. */
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TemplateEngine templateEngine;
    private final String issuerName;
    private final String currency;

    public InvoicePdfRenderer(TemplateEngine templateEngine,
                              @Value("${bms.invoice.issuer-name}") String issuerName,
                              @Value("${bms.invoice.currency}") String currency) {
        this.templateEngine = templateEngine;
        this.issuerName = issuerName;
        this.currency = currency;
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
        context.setVariable("currency", currency);
        context.setVariable("issueDate", DATE.format(invoice.getIssueDate()));
        context.setVariable("dueDate", DATE.format(invoice.getDueDate()));
        context.setVariable("periodStart", DATE.format(invoice.getPeriodStart()));
        context.setVariable("periodEnd", DATE.format(invoice.getPeriodEnd()));

        String html = templateEngine.process("invoice", context);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(output);
        try {
            builder.run();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not render invoice " + invoice.getInvoiceNumber(), exception);
        }
        return output.toByteArray();
    }
}
