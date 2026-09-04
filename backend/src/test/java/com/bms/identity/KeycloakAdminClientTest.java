package com.bms.identity;

import com.bms.common.exception.IdentityProviderException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Keycloak refusing our service account is a fault in our own configuration.
 * It used to reach the browser as a bare 401, which reads as "you are not
 * signed in" and sends whoever is debugging it to the wrong place entirely.
 */
class KeycloakAdminClientTest {

    private static final KeycloakAdminProperties PROPERTIES =
            new KeycloakAdminProperties("http://keycloak:8080", "bms", "bms-backend", "a-secret");

    @Test
    void anUnknownClientIsReportedAsAnIdentityProviderFailure() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.serverUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://keycloak:8080/realms/bms/protocol/openid-connect/token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_client\"}"));
        KeycloakAdminClient client = new KeycloakAdminClient(builder.build(), PROPERTIES);

        assertThatThrownBy(() -> client.resetPassword("kc-id", "new-secret", true))
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

        assertThatThrownBy(() -> client.resetPassword("kc-id", "new-secret", true))
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

        client.resetPassword("kc-id", "new-secret", true);

        // Both calls were made, in order, and neither was turned into a failure.
        server.verify();
    }
}
