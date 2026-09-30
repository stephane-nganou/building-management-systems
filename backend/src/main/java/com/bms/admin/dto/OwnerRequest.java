package com.bms.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Signs an owner up on their behalf. Their password is generated and returned once. */
public record OwnerRequest(
        @NotBlank @Email String email,
        @NotBlank String firstName,
        @NotBlank String lastName) {
}
