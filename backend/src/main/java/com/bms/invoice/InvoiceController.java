package com.bms.invoice;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.bms.access.Permission;
import com.bms.invoice.dto.InvoiceRequest;
import com.bms.invoice.dto.InvoiceResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoices;
    private final InvoicePdfRenderer pdfRenderer;

    public InvoiceController(InvoiceService invoices, InvoicePdfRenderer pdfRenderer) {
        this.invoices = invoices;
        this.pdfRenderer = pdfRenderer;
    }

    @GetMapping
    public List<InvoiceResponse> search(
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) UUID apartmentId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return invoices.search(buildingId, apartmentId, status, from, to);
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable UUID id) {
        return invoices.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@Valid @RequestBody InvoiceRequest request) {
        return invoices.create(request);
    }

    @PostMapping("/{id}/status")
    public InvoiceResponse changeStatus(@PathVariable UUID id, @RequestParam InvoiceStatus status) {
        return invoices.changeStatus(id, status);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        Invoice invoice = invoices.require(id, Permission.INVOICE_READ);
        byte[] pdf = pdfRenderer.render(invoice);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(invoice.getInvoiceNumber() + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        invoices.delete(id);
    }
}
