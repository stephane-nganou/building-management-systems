package com.bms.support;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Builds the Keycloak style tokens the API expects, without running Keycloak.
 *
 * <p>The cross site request forgery token comes along with them, so that the
 * suites below can be about who may do what rather than about forgery. That
 * protection guards the session cookie a browser holds, and it has a test of
 * its own in {@code AuthenticationIntegrationTest}.
 */
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
        RequestPostProcessor authentication = jwt()
                .jwt(builder -> builder
                        .subject(keycloakId)
                        .claim("email", email)
                        .claim("given_name", "Test")
                        .claim("family_name", keycloakId))
                .authorities(new SimpleGrantedAuthority(authority));
        RequestPostProcessor forgeryToken = csrf();
        return request -> forgeryToken.postProcessRequest(authentication.postProcessRequest(request));
    }
}
