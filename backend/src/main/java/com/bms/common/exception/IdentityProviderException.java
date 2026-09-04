package com.bms.common.exception;

/**
 * Raised when Keycloak cannot be used to manage accounts: it is unreachable, or
 * the client we authenticate with is missing or misconfigured.
 *
 * <p>Deliberately distinct from the authentication failures of our own API. A
 * 401 from Keycloak's token endpoint means our server is set up wrong, not that
 * the caller is unauthenticated, and reporting it as the latter sends whoever is
 * debugging it looking in the wrong place.
 */
public class IdentityProviderException extends RuntimeException {

    public IdentityProviderException(String message) {
        super(message);
    }

    public IdentityProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
