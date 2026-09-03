package com.bms.identity;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.bms.common.exception.ValidationException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * The slice of Keycloak's admin REST API this application needs: create an
 * account, set its password and give it a realm role.
 *
 * <p>Written against {@link RestClient} rather than the official admin client,
 * which would pull an entire JAX-RS stack in for four calls.
 */
@Component
public class KeycloakAdminClient {

    private final RestClient http;
    private final KeycloakAdminProperties properties;

    public KeycloakAdminClient(KeycloakAdminProperties properties) {
        this.http = RestClient.create(properties.serverUrl());
        this.properties = properties;
    }

    /**
     * Creates an enabled account and returns its Keycloak id, which is the
     * {@code sub} claim of every token it will later carry.
     */
    public String createUser(String email, String firstName, String lastName, String password,
                             boolean temporaryPassword, String realmRole) {
        String token = accessToken();
        String userId = createAccount(token, email, firstName, lastName);
        setPassword(token, userId, password, temporaryPassword);
        assignRealmRole(token, userId, realmRole);
        return userId;
    }

    public void resetPassword(String keycloakId, String password, boolean temporary) {
        setPassword(accessToken(), keycloakId, password, temporary);
    }

    private String createAccount(String token, String email, String firstName, String lastName) {
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
                        "emailVerified", true))
                .exchange((request, response) -> {
                    if (response.getStatusCode().value() == 409) {
                        throw new ValidationException("An account already exists for " + email);
                    }
                    failOnError(response.getStatusCode(), "create the account");
                    return response.getHeaders().getLocation();
                });
        if (location == null) {
            throw new IllegalStateException("Keycloak created the account but returned no Location header");
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private void setPassword(String token, String userId, String password, boolean temporary) {
        http.put()
                .uri("/admin/realms/{realm}/users/{id}/reset-password", properties.realm(), userId)
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("type", "password", "value", password, "temporary", temporary))
                .exchange((request, response) -> {
                    failOnError(response.getStatusCode(), "set the password");
                    return null;
                });
    }

    private void assignRealmRole(String token, String userId, String role) {
        Map<String, Object> representation = http.get()
                .uri("/admin/realms/{realm}/roles/{role}", properties.realm(), role)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
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
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());

        Map<String, Object> body = http.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return String.valueOf(body.get("access_token"));
    }

    private void failOnError(HttpStatusCode status, String action) {
        if (status.isError()) {
            throw new IllegalStateException("Keycloak refused to " + action + ", status " + status.value());
        }
    }
}
