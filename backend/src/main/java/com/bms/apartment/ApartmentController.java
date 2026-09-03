package com.bms.apartment;

import java.util.List;
import java.util.UUID;

import com.bms.apartment.dto.ApartmentRequest;
import com.bms.apartment.dto.ApartmentResponse;
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
public class ApartmentController {

    private final ApartmentService apartments;

    public ApartmentController(ApartmentService apartments) {
        this.apartments = apartments;
    }

    @GetMapping("/api/apartments")
    public List<ApartmentResponse> list(@RequestParam(required = false) UUID buildingId) {
        return buildingId == null ? apartments.listAll() : apartments.listForBuilding(buildingId);
    }

    @GetMapping("/api/apartments/{id}")
    public ApartmentResponse get(@PathVariable UUID id) {
        return apartments.get(id);
    }

    @PostMapping("/api/buildings/{buildingId}/apartments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApartmentResponse create(@PathVariable UUID buildingId, @Valid @RequestBody ApartmentRequest request) {
        return apartments.create(buildingId, request);
    }

    @PutMapping("/api/apartments/{id}")
    public ApartmentResponse update(@PathVariable UUID id, @Valid @RequestBody ApartmentRequest request) {
        return apartments.update(id, request);
    }

    @DeleteMapping("/api/apartments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        apartments.delete(id);
    }
}
