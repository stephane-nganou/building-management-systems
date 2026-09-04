package com.bms.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.bms.access.AccessControl;
import com.bms.access.Permission;
import com.bms.apartment.Apartment;
import com.bms.common.exception.NotFoundException;
import com.bms.common.exception.ValidationException;
import com.bms.common.i18n.Messages;
import com.bms.invoice.dto.InvoiceRequest;
import com.bms.invoice.dto.InvoiceResponse;
import com.bms.tenant.Tenant;
import com.bms.tenant.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private final InvoiceRepository invoices;
    private final TenantService tenants;
    private final InvoiceNumberGenerator numberGenerator;
    private final AccessControl accessControl;
    private final Messages messages;

    public InvoiceService(InvoiceRepository invoices, TenantService tenants,
                          InvoiceNumberGenerator numberGenerator, AccessControl accessControl,
                          Messages messages) {
        this.invoices = invoices;
        this.tenants = tenants;
        this.numberGenerator = numberGenerator;
        this.accessControl = accessControl;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> search(UUID buildingId, UUID apartmentId, InvoiceStatus status,
                                        LocalDate from, LocalDate to) {
        return invoices.search(accessControl.accessibleOwnerIds(Permission.INVOICE_READ),
                        buildingId, apartmentId, status, from, to)
                .stream().map(InvoiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(UUID id) {
        return InvoiceResponse.from(require(id, Permission.INVOICE_READ));
    }

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        Tenant tenant = tenants.require(request.tenantId(), Permission.INVOICE_WRITE);
        Apartment apartment = tenant.getApartment();

        Invoice invoice = new Invoice(apartment, tenant, numberGenerator.next(request.issueDate()),
                request.type(), request.periodStart(), request.periodEnd(),
                request.issueDate(), request.dueDate(), request.notes());
        addLines(invoice, request, apartment);
        return InvoiceResponse.from(invoices.save(invoice));
    }

    @Transactional
    public InvoiceResponse changeStatus(UUID id, InvoiceStatus status) {
        Invoice invoice = require(id, Permission.INVOICE_WRITE);
        invoice.transitionTo(status);
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public void delete(UUID id) {
        Invoice invoice = require(id, Permission.INVOICE_WRITE);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ValidationException("error.invoice.deleteDraftOnly");
        }
        invoices.delete(invoice);
    }

    @Transactional(readOnly = true)
    public Invoice require(UUID id, Permission permission) {
        return invoices.findByIdAndApartmentBuildingOwnerIdIn(id, accessControl.accessibleOwnerIds(permission))
                .orElseThrow(() -> NotFoundException.of("error.notFound.invoice", id));
    }

    private void addLines(Invoice invoice, InvoiceRequest request, Apartment apartment) {
        if (request.lines() != null && !request.lines().isEmpty()) {
            request.lines().forEach(line ->
                    invoice.addLine(line.description(), line.quantity(), line.unitPrice(), line.unit()));
            return;
        }
        if (request.type() != InvoiceType.RENT) {
            throw new ValidationException("error.invoice.linesRequired",
                    messages.get("invoice.type." + request.type()));
        }
        // Written in the language of whoever created the invoice, and left as
        // written from then on, exactly like a line a user typed themselves.
        String month = messages.get("invoice.line.month");
        invoice.addLine(messages.get("invoice.line.rent", apartment.getLabel()),
                BigDecimal.ONE, apartment.getBaseRent(), month);
        if (apartment.getUtilitiesAdvance().compareTo(BigDecimal.ZERO) > 0) {
            invoice.addLine(messages.get("invoice.line.utilitiesAdvance"),
                    BigDecimal.ONE, apartment.getUtilitiesAdvance(), month);
        }
    }
}
