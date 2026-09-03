package com.bms.building;

import java.util.List;
import java.util.UUID;

import com.bms.building.dto.BuildingRequest;
import com.bms.building.dto.BuildingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    private final BuildingService buildings;

    public BuildingController(BuildingService buildings) {
        this.buildings = buildings;
    }

    @GetMapping
    public List<BuildingResponse> list() {
        return buildings.list();
    }

    @GetMapping("/{id}")
    public BuildingResponse get(@PathVariable UUID id) {
        return buildings.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BuildingResponse create(@Valid @RequestBody BuildingRequest request) {
        return buildings.create(request);
    }

    @PutMapping("/{id}")
    public BuildingResponse update(@PathVariable UUID id, @Valid @RequestBody BuildingRequest request) {
        return buildings.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        buildings.delete(id);
    }
}
