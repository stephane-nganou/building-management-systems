package com.bms.signing;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SigningKeyTest {

    private static final Instant NOW = Instant.parse("2026-10-06T09:30:00Z");
    private static final char[] PASSWORD = "changeit".toCharArray();

    @Test
    void aSelfSignedCertificateNamesItsHolderAndIsValidNow() throws Exception {
        SigningKey key = SigningKey.selfSigned("Building Management", NOW);

        assertThat(key.certificate().getSubjectX500Principal().getName()).isEqualTo("CN=Building Management");
        key.certificate().checkValidity(Date.from(NOW));
        key.certificate().verify(key.certificate().getPublicKey());
        assertThat(key.signatureAlgorithm()).isEqualTo("SHA256withRSA");
    }

    @Test
    void theKeystoresPrivateKeyAndChainAreRead(@TempDir Path directory) throws Exception {
        SigningKey original = SigningKey.selfSigned("Building Management", NOW);
        KeyStore store = emptyStore();
        store.setKeyEntry("signing", original.privateKey(), PASSWORD, original.chain().toArray(Certificate[]::new));

        SigningKey loaded = SigningKey.fromKeystore(save(store, directory), PASSWORD);

        assertThat(loaded.privateKey().getEncoded()).isEqualTo(original.privateKey().getEncoded());
        assertThat(loaded.chain()).containsExactly(original.certificate());
    }

    @Test
    void aKeystoreWithoutAPrivateKeyIsRefused(@TempDir Path directory) throws Exception {
        KeyStore store = emptyStore();
        store.setCertificateEntry("ca", SigningKey.selfSigned("Some CA", NOW).certificate());

        Path keystore = save(store, directory);

        assertThatThrownBy(() -> SigningKey.fromKeystore(keystore, PASSWORD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No private key");
    }

    private static KeyStore emptyStore() throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        return store;
    }

    private static Path save(KeyStore store, Path directory) throws Exception {
        Path path = directory.resolve("signing.p12");
        try (OutputStream output = Files.newOutputStream(path)) {
            store.store(output, PASSWORD);
        }
        return path;
    }
}
