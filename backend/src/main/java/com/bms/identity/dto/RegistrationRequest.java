package com.bms.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Signs a new owner up. Assistants never use this; their owner creates them. */
public record RegistrationRequest(
        @NotBlank @Email String email,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password) {
}
