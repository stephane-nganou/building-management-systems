package com.bms.config;

import java.util.Map;

import com.bms.identity.KeycloakProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

/**
 * The registration the backend uses to sign a browser in.
 *
 * <p>Written out rather than configured with an {@code issuer-uri}, which Spring
 * would resolve by fetching Keycloak's discovery document at startup. That
 * cannot work here for two separate reasons. Startup would wait on Keycloak
 * being reachable, which is the coupling the resource server already avoids.
 * More fundamentally, discovery yields one set of URLs, and this deployment
 * needs two: the browser is sent to Keycloak's public address while the code is
 * exchanged over the container network. Only the endpoints the browser visits
 * carry the public host.
 */
@Configuration
public class KeycloakClientConfig {

    public static final String REGISTRATION_ID = "keycloak";

    /** Where Spring receives the authorization code, under /api so one proxy rule covers it. */
    public static final String CALLBACK_BASE_URI = "/api/auth/callback";

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(KeycloakProperties keycloak,
                                                              FrontendProperties frontend) {
        String publicRealm = keycloak.publicRealmUrl();
        String internalRealm = keycloak.internalRealmUrl();
        return new InMemoryClientRegistrationRepository(ClientRegistration
                .withRegistrationId(REGISTRATION_ID)
                .clientId(keycloak.clientId())
                .clientSecret(keycloak.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                // The browser is redirected here, so it is the frontend's address:
                // nginx forwards /api to this service.
                .redirectUri(frontend.baseUrl() + CALLBACK_BASE_URI + "/{registrationId}")
                .scope("openid", "profile", "email", "roles")
                .authorizationUri(publicRealm + "/protocol/openid-connect/auth")
                .tokenUri(internalRealm + "/protocol/openid-connect/token")
                .userInfoUri(internalRealm + "/protocol/openid-connect/userinfo")
                .jwkSetUri(internalRealm + "/protocol/openid-connect/certs")
                // The issuer a token claims is always the public one, whichever
                // address the token was fetched over.
                .issuerUri(publicRealm)
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .providerConfigurationMetadata(Map.of(
                        "end_session_endpoint", publicRealm + "/protocol/openid-connect/logout"))
                .clientName("Building Management")
                .build());
    }
}
