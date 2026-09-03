package com.bms.support;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** Builds the Keycloak style tokens the API expects, without running Keycloak. */
public final class Jwts {

    private Jwts() {
    }

    public static RequestPostProcessor asUser(String keycloakId, String email) {
        return jwt().jwt(builder -> builder
                .subject(keycloakId)
                .claim("email", email)
                .claim("given_name", "Test")
                .claim("family_name", keycloakId));
    }
}
