package com.bms.signing;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/** A private key and the certificate chain that names its holder, signing certificate first. */
public record SigningKey(PrivateKey privateKey, List<X509Certificate> chain) {

    public X509Certificate certificate() {
        return chain.getFirst();
    }

    /** The JCA name of the algorithm this key signs with, over a SHA-256 digest. */
    public String signatureAlgorithm() {
        String algorithm = privateKey.getAlgorithm();
        return "SHA256with" + ("EC".equals(algorithm) ? "ECDSA" : algorithm);
    }

    /** The keystore's first private key entry. */
    public static SigningKey fromKeystore(Path path, char[] password) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (InputStream stream = Files.newInputStream(path)) {
            keystore.load(stream, password);
        }
        for (String alias : Collections.list(keystore.aliases())) {
            if (keystore.isKeyEntry(alias)) {
                List<X509Certificate> chain = Arrays.stream(keystore.getCertificateChain(alias))
                        .map(X509Certificate.class::cast).toList();
                return new SigningKey((PrivateKey) keystore.getKey(alias, password), chain);
            }
        }
        throw new IllegalStateException("No private key in keystore " + path);
    }

    /** A fresh RSA key and a certificate it signs itself, naming {@code holder}. */
    public static SigningKey selfSigned(String holder, Instant now) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);
        KeyPair pair = generator.generateKeyPair();

        X500Name name = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, holder).build();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name,
                BigInteger.valueOf(now.toEpochMilli()), Date.from(now.minus(Duration.ofDays(1))),
                Date.from(now.plus(Duration.ofDays(3650))), name, pair.getPublic());
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(
                builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate())));
        return new SigningKey(pair.getPrivate(), List.of(certificate));
    }
}
