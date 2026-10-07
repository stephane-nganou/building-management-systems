package com.bms.signing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * The key issued invoices are signed with.
 *
 * <p>Production mounts a PKCS12 keystore holding one private key and its
 * certificate chain, ideally from a certificate authority PDF readers trust.
 * Without one the application refuses to start, unless {@code allowSelfSigned}
 * says this is a development machine: it then makes itself a certificate at
 * every start, which proves nothing to anyone outside. A keystore without its
 * password is refused too, rather than failing on the first read.
 */
@ConfigurationProperties(prefix = "bms.signing")
public record SigningProperties(String keystorePath, String keystorePassword, boolean allowSelfSigned) {

    public SigningProperties {
        if (!StringUtils.hasText(keystorePath) && !allowSelfSigned) {
            throw new IllegalStateException("bms.signing.keystore-path is blank; set BMS_SIGNING_KEYSTORE_PATH, "
                    + "or BMS_SIGNING_ALLOW_SELF_SIGNED=true on a development machine");
        }
        if (StringUtils.hasText(keystorePath) && !StringUtils.hasText(keystorePassword)) {
            throw new IllegalStateException("bms.signing.keystore-password is missing; set BMS_SIGNING_KEYSTORE_PASSWORD");
        }
    }

    public boolean hasKeystore() {
        return StringUtils.hasText(keystorePath);
    }
}
