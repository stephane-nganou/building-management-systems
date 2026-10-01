package com.bms.identity;

import java.util.Map;

import com.bms.common.exception.IdentityProviderException;
import com.bms.common.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Keycloak refusing our service account is a fault in our own configuration.
 * It used to reach the browser as a bare 401, which reads as "you are not
 * signed in" and sends whoever is debugging it to the wrong place entirely.
 */
class KeycloakAdminClientTest {

    private static final KeycloakProperties PROPERTIES =
            new KeycloakProperties("http://keycloak:8080", "http://localhost:8081", "bms", "bms-backend", "a-secret", false);

    @Test
    void anUnknownClientIsReportedAsAnIdentityProviderFailure() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.serverUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://keycloak:8080/realms/bms/protocol/openid-connect/token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_client\"}"));
        KeycloakAdminClient client = new KeycloakAdminClient(builder.build(), PROPERTIES);

        assertThatThrownBy(() -> client.resetPassword("kc-id", "new-secret"))
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("bms-backend")
                .hasMessageContaining("secret matches");
    }

    @Test
    void anUnreachableKeycloakIsReportedTheSameWay() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.serverUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://keycloak:8080/realms/bms/protocol/openid-connect/token"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        KeycloakAdminClient client = new KeycloakAdminClient(builder.build(), PROPERTIES);

        assertThatThrownBy(() -> client.resetPassword("kc-id", "new-secret"))
                .isInstanceOf(IdentityProviderException.class);
    }

    @Test
    void aWorkingTokenLetsThePasswordThrough() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.serverUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://keycloak:8080/realms/bms/protocol/openid-connect/token"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"access_token\":\"a-token\"}"));
        server.expect(requestTo("http://keycloak:8080/admin/realms/bms/users/kc-id/reset-password"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        KeycloakAdminClient client = new KeycloakAdminClient(builder.build(), PROPERTIES);

        client.resetPassword("kc-id", "new-secret");

        // Both calls were made, in order, and neither was turned into a failure.
        server.verify();
    }

    @Test
    void disablingSendsOnlyTheEnabledFlag() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.serverUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://keycloak:8080/realms/bms/protocol/openid-connect/token"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"access_token\":\"a-token\"}"));
        server.expect(requestTo("http://keycloak:8080/admin/realms/bms/users/kc-id"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("{\"enabled\":false}", JsonCompareMode.STRICT))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        KeycloakAdminClient client = new KeycloakAdminClient(builder.build(), PROPERTIES);

        client.setEnabled("kc-id", false);

        server.verify();
    }

    @Test
    void aWrongCurrentPasswordIsTheCallersMistake() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.serverUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://keycloak:8080/realms/bms/protocol/openid-connect/token"))
                .andExpect(content().formDataContains(Map.of(
                        "grant_type", "password",
                        "client_id", "bms-password-check",
                        "username", "someone@example.com")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\"}"));
        KeycloakAdminClient client = new KeycloakAdminClient(builder.build(), PROPERTIES);

        assertThatThrownBy(() -> client.verifyPassword("someone@example.com", "not-it"))
                .isInstanceOf(ValidationException.class)
                .extracting("code").isEqualTo("error.password.current");
    }

    @Test
    void aRightCurrentPasswordLeavesNoSessionBehind() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.serverUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://keycloak:8080/realms/bms/protocol/openid-connect/token"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"access_token\":\"a-token\",\"refresh_token\":\"a-refresh-token\"}"));
        server.expect(requestTo("http://keycloak:8080/realms/bms/protocol/openid-connect/logout"))
                .andExpect(content().formDataContains(Map.of("refresh_token", "a-refresh-token")))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        KeycloakAdminClient client = new KeycloakAdminClient(builder.build(), PROPERTIES);

        client.verifyPassword("someone@example.com", "the-right-one");

        server.verify();
    }

    @Test
    void anUnknownPasswordCheckClientIsOurFaultNotTheUsers() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.serverUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://keycloak:8080/realms/bms/protocol/openid-connect/token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_client\"}"));
        KeycloakAdminClient client = new KeycloakAdminClient(builder.build(), PROPERTIES);

        assertThatThrownBy(() -> client.verifyPassword("someone@example.com", "the-right-one"))
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("bms-password-check");
    }

    @Test
    void anAccountWithAStepPendingIsToldSo() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.serverUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://keycloak:8080/realms/bms/protocol/openid-connect/token"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\",\"error_description\":\"Account is not fully set up\"}"));
        KeycloakAdminClient client = new KeycloakAdminClient(builder.build(), PROPERTIES);

        assertThatThrownBy(() -> client.verifyPassword("someone@example.com", "the-right-one"))
                .isInstanceOf(ValidationException.class)
                .extracting("code").isEqualTo("error.password.accountNotReady");
    }
}
