package com.bms.identity;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.bms.common.exception.IdentityProviderException;
import com.bms.common.exception.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * The slice of Keycloak's admin REST API this application needs: create an
 * account, set its password, give it a realm role, and disable it. Besides the
 * admin API, it checks a user's current password by signing in with it through
 * a second client.
 *
 * <p>Written against {@link RestClient} rather than the official admin client,
 * which would pull an entire JAX-RS stack in for a handful of calls.
 */
@Component
public class KeycloakAdminClient {

    /** Direct access grants only, and holding the same secret as the main client; see realm-bms.json. */
    private static final String PASSWORD_CHECK_CLIENT_ID = "bms-password-check";

    private static final String TOKEN_PATH = "/realms/{realm}/protocol/openid-connect/token";

    /** Keycloak's description of a sign in refused while a required action is pending. */
    private static final String ACCOUNT_NOT_SET_UP = "Account is not fully set up";

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient http;
    private final KeycloakProperties properties;

    // Named explicitly: a second constructor exists for tests, and Spring will not
    // guess between them.
    @Autowired
    public KeycloakAdminClient(KeycloakProperties properties) {
        this(RestClient.create(properties.serverUrl()), properties);
    }

    /** Lets a test supply a client bound to a mock server. */
    KeycloakAdminClient(RestClient http, KeycloakProperties properties) {
        this.http = http;
        this.properties = properties;
    }

    /**
     * Creates an enabled account and returns its Keycloak id, which is the
     * {@code sub} claim of every token it will later carry.
     */
    /**
     * Creates the account with its password in one call, so a password the realm's
     * policy refuses leaves no account behind. An unverified account is asked by
     * Keycloak to confirm its address at the first sign in.
     */
    public String createUser(String email, String firstName, String lastName, String password, String realmRole,
                             boolean emailVerified) {
        String token = accessToken();
        String userId = createAccount(token, email, firstName, lastName, password, emailVerified);
        assignRealmRole(token, userId, realmRole);
        return userId;
    }

    public void resetPassword(String keycloakId, String password) {
        setPassword(accessToken(), keycloakId, password);
    }

    /**
     * Proves a user knows their current password by signing in with it, through
     * a client that allows nothing else. A wrong password counts towards the
     * realm's lockout like any failed sign in. The session that sign in opened is
     * ended at once.
     */
    public void verifyPassword(String username, String password) {
        MultiValueMap<String, String> form = clientForm(PASSWORD_CHECK_CLIENT_ID);
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);
        Map<String, Object> tokens = http.post()
                .uri(TOKEN_PATH, properties.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .exchange((request, response) -> {
                    if (response.getStatusCode().is4xxClientError()) {
                        failOnRefusedSignIn(response.bodyTo(JSON_OBJECT));
                    }
                    failOnError(response.getStatusCode(),
                            "check the current password through the '" + PASSWORD_CHECK_CLIENT_ID
                                    + "' client; check that it exists and its secret matches");
                    return response.bodyTo(JSON_OBJECT);
                });
        endSession(String.valueOf(tokens.get("refresh_token")));
    }

    /**
     * Keycloak answers {@code invalid_grant} to a sign in it refuses for the
     * user's sake. Every other refusal, {@code invalid_client} above all, is our
     * configuration and is left to {@link #failOnError}. A locked out account is
     * deliberately told apart from a wrong password by nobody, Keycloak included.
     */
    private void failOnRefusedSignIn(Map<String, Object> error) {
        if (!"invalid_grant".equals(error.get("error"))) {
            return;
        }
        if (ACCOUNT_NOT_SET_UP.equals(error.get("error_description"))) {
            throw new ValidationException("error.password.accountNotReady");
        }
        throw new ValidationException("error.password.current");
    }

    private void endSession(String refreshToken) {
        MultiValueMap<String, String> form = clientForm(PASSWORD_CHECK_CLIENT_ID);
        form.add("refresh_token", refreshToken);
        http.post()
                .uri("/realms/{realm}/protocol/openid-connect/logout", properties.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .exchange((request, response) -> {
                    failOnError(response.getStatusCode(), "end the password check session");
                    return null;
                });
    }

    /** Both clients hold the same secret. */
    private MultiValueMap<String, String> clientForm(String clientId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", properties.clientSecret());
        return form;
    }

    /** A disabled account cannot sign in. Keycloak only changes the fields it is sent. */
    public void setEnabled(String keycloakId, boolean enabled) {
        http.put()
                .uri("/admin/realms/{realm}/users/{id}", properties.realm(), keycloakId)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("enabled", enabled))
                .exchange((request, response) -> {
                    failOnError(response.getStatusCode(), enabled ? "enable the account" : "disable the account");
                    return null;
                });
    }

    private String createAccount(String token, String email, String firstName, String lastName, String password,
                                 boolean emailVerified) {
        URI location = http.post()
                .uri("/admin/realms/{realm}/users", properties.realm())
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "username", email,
                        "email", email,
                        "firstName", firstName,
                        "lastName", lastName,
                        "enabled", true,
                        "emailVerified", emailVerified,
                        "credentials", List.of(Map.of("type", "password", "value", password, "temporary", false))))
                .exchange((request, response) -> {
                    if (response.getStatusCode().value() == 409) {
                        throw new ValidationException("error.account.exists", email);
                    }
                    failOnPasswordPolicy(response.getStatusCode());
                    failOnError(response.getStatusCode(), "create the account");
                    return response.getHeaders().getLocation();
                });
        if (location == null) {
            throw new IllegalStateException("Keycloak created the account but returned no Location header");
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * Always a permanent password. Keycloak's temporary ones are discharged on
     * its own account pages, which this application never sends anyone to; where
     * a password still has to be replaced, {@code app_user} records it.
     */
    private void setPassword(String token, String userId, String password) {
        http.put()
                .uri("/admin/realms/{realm}/users/{id}/reset-password", properties.realm(), userId)
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("type", "password", "value", password, "temporary", false))
                .exchange((request, response) -> {
                    failOnPasswordPolicy(response.getStatusCode());
                    failOnError(response.getStatusCode(), "set the password");
                    return null;
                });
    }

    /**
     * Keycloak answers 400 to a password its policy refuses. Our own validation
     * mirrors that policy, so this is the backstop for a rule it does not know.
     */
    private void failOnPasswordPolicy(HttpStatusCode status) {
        if (status.value() == 400) {
            throw new ValidationException("error.password.policy");
        }
    }

    private void assignRealmRole(String token, String userId, String role) {
        Map<String, Object> representation;
        try {
            representation = http.get()
                    .uri("/admin/realms/{realm}/roles/{role}", properties.realm(), role)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(JSON_OBJECT);
        } catch (RestClientException exception) {
            throw new IdentityProviderException(
                    "Could not read the realm role '" + role + "' from realm '" + properties.realm() + "'",
                    exception);
        }
        http.post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", properties.realm(), userId)
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(representation))
                .exchange((request, response) -> {
                    failOnError(response.getStatusCode(), "assign the " + role + " role");
                    return null;
                });
    }

    private String accessToken() {
        MultiValueMap<String, String> form = clientForm(properties.clientId());
        form.add("grant_type", "client_credentials");

        Map<String, Object> body;
        try {
            body = http.post()
                    .uri(TOKEN_PATH, properties.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JSON_OBJECT);
        } catch (RestClientException exception) {
            // Almost always the realm was imported before this client existed:
            // Keycloak only imports a realm that is not already in its database.
            throw new IdentityProviderException(
                    "Could not authenticate as the '" + properties.clientId() + "' client in realm '"
                            + properties.realm() + "'. Check that the client exists and its secret matches.",
                    exception);
        }
        return String.valueOf(body.get("access_token"));
    }

    private void failOnError(HttpStatusCode status, String action) {
        if (status.isError()) {
            throw new IdentityProviderException("Keycloak refused to " + action + ", status " + status.value());
        }
    }
}
