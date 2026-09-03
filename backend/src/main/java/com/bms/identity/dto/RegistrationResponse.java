package com.bms.identity.dto;

import java.util.UUID;

public record RegistrationResponse(UUID id, String email, String name) {
}
