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
 * <p>One confidential client, {@code bms-backend}, drives the authorization
 * code flow for the browser and holds the service account that creates
 * accounts. A second, {@code bms-password-check}, shares its secret and only
 * confirms a current password.
 *
 * <p>The client secret has a development default, committed to the repository.
 * Starting with that, or with none at all, fails unless
 * {@code allowDefaultSecret} says this is a development machine: a blank
 * secret would only show at the first sign in, and the default one is public.
 */
@ConfigurationProperties(prefix = "bms.keycloak")
public record KeycloakProperties(
        String serverUrl,
        String publicUrl,
        String realm,
        String clientId,
        String clientSecret,
        boolean allowDefaultSecret) {

    public static final String DEVELOPMENT_SECRET = "bms-backend-secret";

    public KeycloakProperties {
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("bms.keycloak.client-secret is blank; set BMS_KEYCLOAK_CLIENT_SECRET");
        }
        if (DEVELOPMENT_SECRET.equals(clientSecret) && !allowDefaultSecret) {
            throw new IllegalStateException("bms.keycloak.client-secret is the committed development default; "
                    + "set BMS_KEYCLOAK_CLIENT_SECRET, or BMS_KEYCLOAK_ALLOW_DEFAULT_SECRET=true on a development machine");
        }
    }

    /** The realm as the browser and every token issuer claim name it. */
    public String publicRealmUrl() {
        return publicUrl + "/realms/" + realm;
    }

    /** The realm as this process reaches it. */
    public String internalRealmUrl() {
        return serverUrl + "/realms/" + realm;
    }
}
