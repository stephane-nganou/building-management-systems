package com.bms.signing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The application refuses to sign with a certificate it made itself, unless told this is development. */
class SigningPropertiesTest {

    @Test
    void noKeystoreFailsStartupOutsideDevelopment() {
        assertThatThrownBy(() -> new SigningProperties(" ", null, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BMS_SIGNING_KEYSTORE_PATH");
    }

    @Test
    void aKeystoreWithoutItsPasswordFailsStartup() {
        // An unset environment variable reaches the record as a blank string.
        assertThatThrownBy(() -> new SigningProperties("/run/secrets/signing.p12", "", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BMS_SIGNING_KEYSTORE_PASSWORD");
    }

    @Test
    void noKeystoreIsAcceptedOnADevelopmentMachine() {
        assertThat(new SigningProperties("", null, true).hasKeystore()).isFalse();
    }

    @Test
    void aKeystoreIsAccepted() {
        assertThat(new SigningProperties("/run/secrets/signing.p12", "secret", false).hasKeystore()).isTrue();
    }
}
