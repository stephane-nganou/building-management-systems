package com.bms.access;

import java.util.List;
import java.util.UUID;

import com.bms.access.dto.AssistantRequest;
import com.bms.access.dto.AssistantResponse;
import com.bms.access.dto.PermissionsRequest;
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
@RequestMapping("/api/assistants")
public class AssistantController {

    private final AssistantService assistants;

    public AssistantController(AssistantService assistants) {
        this.assistants = assistants;
    }

    @GetMapping
    public List<AssistantResponse> list() {
        return assistants.list();
    }

    @GetMapping("/permissions")
    public Permission[] availablePermissions() {
        return Permission.values();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssistantResponse grant(@Valid @RequestBody AssistantRequest request) {
        return assistants.grant(request);
    }

    @PutMapping("/{id}")
    public AssistantResponse update(@PathVariable UUID id, @Valid @RequestBody PermissionsRequest request) {
        return assistants.updatePermissions(id, request);
    }

    /** Issues a new temporary password, returned once so the owner can pass it on. */
    @PostMapping("/{id}/password")
    public AssistantResponse resetPassword(@PathVariable UUID id) {
        return assistants.resetPassword(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id) {
        assistants.revoke(id);
    }
}
