package com.bms.tenant;

import java.util.List;
import java.util.UUID;

import com.bms.tenant.dto.TenantRequest;
import com.bms.tenant.dto.TenantResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantController {

    private final TenantService tenants;

    public TenantController(TenantService tenants) {
        this.tenants = tenants;
    }

    @GetMapping("/api/tenants")
    public List<TenantResponse> list(@RequestParam(required = false) UUID apartmentId) {
        return apartmentId == null ? tenants.listAll() : tenants.listForApartment(apartmentId);
    }

    @GetMapping("/api/tenants/{id}")
    public TenantResponse get(@PathVariable UUID id) {
        return tenants.get(id);
    }

    @PostMapping("/api/apartments/{apartmentId}/tenants")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@PathVariable UUID apartmentId, @Valid @RequestBody TenantRequest request) {
        return tenants.create(apartmentId, request);
    }

    @PutMapping("/api/tenants/{id}")
    public TenantResponse update(@PathVariable UUID id, @Valid @RequestBody TenantRequest request) {
        return tenants.update(id, request);
    }

    @DeleteMapping("/api/tenants/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        tenants.delete(id);
    }
}
