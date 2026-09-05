package com.bms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where the browser application lives. Sign in, sign out and CORS all end up
 * sending someone back to it, and every one of those redirects is built from
 * this value rather than from the incoming request, so a forged {@code Host}
 * header cannot turn a redirect into somebody else's site.
 */
@ConfigurationProperties(prefix = "bms.frontend")
public record FrontendProperties(String baseUrl) {

    /** Normalised once, so nothing downstream has to wonder about a trailing slash. */
    public FrontendProperties {
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * The application's home page, with its trailing slash. Keycloak matches a
     * redirect against a registered pattern, and a bare authority with no path
     * at all is the one form those patterns tend not to cover.
     */
    public String homeUrl() {
        return baseUrl + "/";
    }
}
