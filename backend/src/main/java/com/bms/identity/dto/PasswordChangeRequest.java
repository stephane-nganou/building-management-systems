package com.bms.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Chooses a new password for the signed in account.
 *
 * <p>The current one is not asked for. Proving it would mean sending it back to
 * Keycloak through the direct access grant this application deliberately does
 * not enable, and the caller has already proved as much as that would: a session
 * cookie no script can read, or an access token of their own.
 */
public record PasswordChangeRequest(
        @NotBlank @Size(min = 8, message = "must be at least 8 characters") String newPassword) {
}
