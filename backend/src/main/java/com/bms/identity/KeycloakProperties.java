package com.bms.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where Keycloak is and who we are to it.
 *
 * <p>Two URLs, not one, and the difference matters. {@code serverUrl} is how
 * this process reaches Keycloak, over the container network. {@code publicUrl}
 * is how the browser reaches it, and so is the host in every token's
 * {@code iss} claim. Sending the browser to the internal name would fail to
 * resolve; validating a token against it would fail to match.
 *
 * <p>One confidential client, {@code bms-backend}, does everything: it drives
 * the authorization code flow for the browser and holds the service account
 * that creates accounts.
 */
@ConfigurationProperties(prefix = "bms.keycloak")
public record KeycloakProperties(
        String serverUrl,
        String publicUrl,
        String realm,
        String clientId,
        String clientSecret) {

    /** The realm as the browser and every token issuer claim name it. */
    public String publicRealmUrl() {
        return publicUrl + "/realms/" + realm;
    }

    /** The realm as this process reaches it. */
    public String internalRealmUrl() {
        return serverUrl + "/realms/" + realm;
    }
}
