package com.bms.config;

import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Carries the reader's language out to the sign in page.
 *
 * <p>Keycloak's own pages speak both languages, and {@code ui_locales} is the
 * standard way to say which. Without this the application would switch to
 * French and then hand the reader an English sign in page.
 *
 * <p>The value is checked against the languages we actually have rather than
 * passed on as it arrives, so a crafted link cannot append parameters of its own
 * choosing to the authorization request.
 */
class LocalizedAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String UI_LOCALES = "ui_locales";
    private static final Set<String> SUPPORTED = Set.of("en", "fr");

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    LocalizedAuthorizationRequestResolver(ClientRegistrationRepository clients, String authorizationBaseUri) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clients, authorizationBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return localize(delegate.resolve(request), request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return localize(delegate.resolve(request, clientRegistrationId), request);
    }

    private OAuth2AuthorizationRequest localize(OAuth2AuthorizationRequest authorization,
                                                HttpServletRequest request) {
        String language = request.getParameter(UI_LOCALES);
        if (authorization == null || language == null || !SUPPORTED.contains(language)) {
            return authorization;
        }
        return OAuth2AuthorizationRequest.from(authorization)
                .additionalParameters(parameters -> parameters.put(UI_LOCALES, language))
                .build();
    }
}
