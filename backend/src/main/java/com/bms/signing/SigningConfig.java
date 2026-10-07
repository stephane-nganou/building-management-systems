package com.bms.signing;

import java.nio.file.Path;
import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The signing key, read once at startup: a keystore that cannot be opened stops
 * the application there, rather than at the first download of an invoice.
 * Production reads the mounted keystore; a development machine without one makes
 * itself a certificate, and says so in the log.
 */
@Configuration
public class SigningConfig {

    private static final Logger log = LoggerFactory.getLogger(SigningConfig.class);

    @Bean
    SigningKey signingKey(SigningProperties properties, @Value("${bms.invoice.issuer-name}") String issuerName,
                          Clock clock) throws Exception {
        if (properties.hasKeystore()) {
            return SigningKey.fromKeystore(Path.of(properties.keystorePath()),
                    properties.keystorePassword().toCharArray());
        }
        log.warn("Signing invoices with a self-signed certificate made at startup; "
                + "set BMS_SIGNING_KEYSTORE_PATH in production");
        return SigningKey.selfSigned(issuerName, clock.instant());
    }
}
