package com.bms.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Chooses a new password for the signed in account.
 *
 * <p>The current one is asked for, so that a session someone else got hold of
 * cannot be turned into the account itself. For an account given a password
 * to replace, the current one is the password it was handed.
 */
public record PasswordChangeRequest(
        @NotBlank @Size(max = 128) String currentPassword,
        @NotBlank @Size(min = 12, max = 128, message = "must be 12 to 128 characters") String newPassword) {
}
