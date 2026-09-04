package com.bms.access.dto;

import java.util.Set;

import com.bms.access.Permission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Gives someone assistant access to the caller's data. When no account exists
 * for the email yet, one is created and its password returned once to the owner.
 */
public record AssistantRequest(
        @NotBlank @Email String email,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull Set<Permission> permissions) {
}
