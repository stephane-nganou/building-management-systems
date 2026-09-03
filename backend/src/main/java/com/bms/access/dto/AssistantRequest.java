package com.bms.access.dto;

import java.util.Set;

import com.bms.access.Permission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Grants an existing user assistant access to the caller's data.
 * The assistant must have signed in at least once so that a local account exists.
 */
public record AssistantRequest(
        @NotBlank @Email String email,
        @NotNull Set<Permission> permissions) {
}
