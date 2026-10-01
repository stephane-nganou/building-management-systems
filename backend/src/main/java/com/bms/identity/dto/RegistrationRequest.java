package com.bms.identity.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Signs a new owner up. Assistants never use this; their owner creates them.
 *
 * <p>The password rules mirror the realm's policy, so a refusal is explained here
 * rather than coming back from Keycloak.
 */
public record RegistrationRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @NotBlank @Size(min = 12, max = 128, message = "must be 12 to 128 characters") String password) {

    /** The email is also the username, and the realm refuses a password equal to either. */
    @AssertTrue(message = "must not be your email address")
    public boolean isPasswordNotTheEmail() {
        return password == null || email == null || !password.equalsIgnoreCase(email);
    }
}
