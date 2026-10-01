package com.bms.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The application refuses to start with a client secret anyone could know. */
class KeycloakPropertiesTest {

    @Test
    void aBlankSecretFailsStartup() {
        assertThatThrownBy(() -> withSecret(" ", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void theCommittedDefaultFailsStartupOutsideDevelopment() {
        assertThatThrownBy(() -> withSecret(KeycloakProperties.DEVELOPMENT_SECRET, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("development default");
    }

    @Test
    void theCommittedDefaultIsAcceptedOnADevelopmentMachine() {
        assertThat(withSecret(KeycloakProperties.DEVELOPMENT_SECRET, true).clientSecret())
                .isEqualTo(KeycloakProperties.DEVELOPMENT_SECRET);
    }

    @Test
    void aRealSecretIsAccepted() {
        assertThat(withSecret("a-generated-secret", false).clientSecret()).isEqualTo("a-generated-secret");
    }

    private static KeycloakProperties withSecret(String secret, boolean allowDefaultSecret) {
        return new KeycloakProperties("http://keycloak:8080", "http://localhost:8081", "bms", "bms-backend",
                secret, allowDefaultSecret);
    }
}
