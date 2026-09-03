package com.bms.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credentials of the confidential client the backend uses to create accounts in
 * Keycloak. Separate from the resource server settings, which only validate
 * tokens and need no secret.
 */
@ConfigurationProperties(prefix = "bms.keycloak.admin")
public record KeycloakAdminProperties(
        String serverUrl,
        String realm,
        String clientId,
        String clientSecret) {
}
