package com.bms.support;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** Builds the Keycloak style tokens the API expects, without running Keycloak. */
public final class Jwts {

    private Jwts() {
    }

    /** Someone who manages their own buildings. */
    public static RequestPostProcessor asUser(String keycloakId, String email) {
        return withRole(keycloakId, email, "ROLE_OWNER");
    }

    /** Someone created by an owner, who only ever works on delegated data. */
    public static RequestPostProcessor asAssistant(String keycloakId, String email) {
        return withRole(keycloakId, email, "ROLE_ASSISTANT");
    }

    private static RequestPostProcessor withRole(String keycloakId, String email, String authority) {
        return jwt()
                .jwt(builder -> builder
                        .subject(keycloakId)
                        .claim("email", email)
                        .claim("given_name", "Test")
                        .claim("family_name", keycloakId))
                .authorities(new SimpleGrantedAuthority(authority));
    }
}
