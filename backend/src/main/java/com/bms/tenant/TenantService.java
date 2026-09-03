package com.bms.tenant;

import java.util.List;
import java.util.UUID;

import com.bms.access.AccessControl;
import com.bms.access.Permission;
import com.bms.apartment.Apartment;
import com.bms.apartment.ApartmentService;
import com.bms.apartment.ApartmentStatus;
import com.bms.common.exception.NotFoundException;
import com.bms.common.exception.ValidationException;
import com.bms.tenant.dto.TenantRequest;
import com.bms.tenant.dto.TenantResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private final TenantRepository tenants;
    private final ApartmentService apartments;
    private final AccessControl accessControl;

    public TenantService(TenantRepository tenants, ApartmentService apartments, AccessControl accessControl) {
        this.tenants = tenants;
        this.apartments = apartments;
        this.accessControl = accessControl;
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> listAll() {
        return tenants.findByApartmentBuildingOwnerIdInOrderByLastNameAsc(
                        accessControl.accessibleOwnerIds(Permission.TENANT_READ))
                .stream().map(TenantResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> listForApartment(UUID apartmentId) {
        apartments.require(apartmentId, Permission.TENANT_READ);
        return tenants.findByApartmentIdOrderByLastNameAsc(apartmentId).stream()
                .map(TenantResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TenantResponse get(UUID id) {
        return TenantResponse.from(require(id, Permission.TENANT_READ));
    }

    @Transactional
    public TenantResponse create(UUID apartmentId, TenantRequest request) {
        Apartment apartment = apartments.require(apartmentId, Permission.TENANT_WRITE);
        validateLease(request);
        if (request.active() && tenants.existsByApartmentIdAndActiveTrue(apartmentId)) {
            throw new ValidationException("This apartment already has an active tenant");
        }
        Tenant tenant = new Tenant(apartment, request.firstName(), request.lastName(), request.email(),
                request.phone(), request.leaseStart(), request.leaseEnd(), request.deposit());
        Tenant saved = tenants.save(tenant);
        if (request.active()) {
            apartment.changeStatus(ApartmentStatus.OCCUPIED);
        }
        return TenantResponse.from(saved);
    }

    @Transactional
    public TenantResponse update(UUID id, TenantRequest request) {
        Tenant tenant = require(id, Permission.TENANT_WRITE);
        validateLease(request);
        tenant.update(request.firstName(), request.lastName(), request.email(), request.phone(),
                request.leaseStart(), request.leaseEnd(), request.deposit(), request.active());
        return TenantResponse.from(tenant);
    }

    @Transactional
    public void delete(UUID id) {
        tenants.delete(require(id, Permission.TENANT_WRITE));
    }

    @Transactional(readOnly = true)
    public Tenant require(UUID id, Permission permission) {
        return tenants.findByIdAndApartmentBuildingOwnerIdIn(id, accessControl.accessibleOwnerIds(permission))
                .orElseThrow(() -> NotFoundException.of("Tenant", id));
    }

    private void validateLease(TenantRequest request) {
        if (request.leaseEnd() != null && request.leaseEnd().isBefore(request.leaseStart())) {
            throw new ValidationException("Lease end must not be before lease start");
        }
    }
}
