package com.bms.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * One API, two ways to prove who you are.
 *
 * <p>A browser signs in through this service: it is sent to Keycloak, comes back
 * to the callback below, and is given a session cookie. No token ever reaches
 * the page, so an injected script has nothing to steal that would outlive the
 * tab. Any other client, a mobile application above all, presents its own access
 * token instead and reaches exactly the same endpoints. Both end up as an
 * authenticated principal carrying the same realm roles.
 *
 * <p>Nothing here ever redirects an unauthenticated caller. The entry point
 * answers 401 and the application decides what to do with it, because a 302
 * towards Keycloak is unreadable to a background request.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Where the browser starts the flow: /api/auth/login/keycloak. */
    private static final String AUTHORIZATION_BASE_URI = "/api/auth/login";

    private static final String LOGOUT_URI = "/api/auth/logout";

    private static final String[] PUBLIC_PATHS = {
            "/api/auth/register",
            AUTHORIZATION_BASE_URI + "/**",
            KeycloakClientConfig.CALLBACK_BASE_URI + "/**",
            LOGOUT_URI,
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final List<String> allowedOrigins;
    private final FrontendProperties frontend;

    public SecurityConfig(@Value("${bms.cors.allowed-origins}") List<String> allowedOrigins,
                          FrontendProperties frontend) {
        this.allowedOrigins = allowedOrigins;
        this.frontend = frontend;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, KeycloakJwtAuthenticationConverter converter,
                                            KeycloakAuthoritiesMapper authoritiesMapper,
                                            ClientRegistrationRepository clients) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(csrfTokenRequestHandler())
                        // A caller holding a bearer token brought its own credential
                        // rather than an ambient cookie, so no other site can make
                        // that request on its behalf. Registration is open to
                        // everyone and rides no session at all.
                        .ignoringRequestMatchers(request -> request.getHeader(HttpHeaders.AUTHORIZATION) != null)
                        .ignoringRequestMatchers("/api/auth/register"))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new LoginReturnPathFilter(AUTHORIZATION_BASE_URI),
                        OAuth2AuthorizationRequestRedirectFilter.class)
                .oauth2Login(login -> login
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri(AUTHORIZATION_BASE_URI)
                                .authorizationRequestResolver(
                                        new LocalizedAuthorizationRequestResolver(clients, AUTHORIZATION_BASE_URI)))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri(
                                KeycloakClientConfig.CALLBACK_BASE_URI + "/*"))
                        .userInfoEndpoint(endpoint -> endpoint.userAuthoritiesMapper(authoritiesMapper))
                        .successHandler(new LoginSuccessHandler(frontend))
                        .failureHandler(new SimpleUrlAuthenticationFailureHandler(
                                frontend.homeUrl() + "?error=signin")))
                .logout(logout -> logout
                        // A GET, because ending a session at Keycloak is a browser
                        // navigation rather than a background call. Nothing is lost
                        // by a forged sign out beyond the annoyance of one.
                        .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults()
                                .matcher(HttpMethod.GET, LOGOUT_URI))
                        .logoutSuccessHandler(logoutSuccessHandler(clients)))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        return http.build();
    }

    /** Ends the Keycloak session too, then returns the browser to the application. */
    private OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler(ClientRegistrationRepository clients) {
        OidcClientInitiatedLogoutSuccessHandler handler = new OidcClientInitiatedLogoutSuccessHandler(clients);
        handler.setPostLogoutRedirectUri(frontend.homeUrl());
        return handler;
    }

    /** Readable by the application's own scripts, which is how it gets into the header. */
    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.path("/"));
        return repository;
    }

    /**
     * The plain handler rather than the XOR encoding default. The encoded form
     * expects the token to be rendered into a server side template, and there is
     * none: the value has to survive a round trip through a cookie that a script
     * reads back unchanged.
     */
    private CsrfTokenRequestAttributeHandler csrfTokenRequestHandler() {
        CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
        // Loads the token on every request, so the cookie is there before the
        // first write, instead of only once something has asked for it.
        handler.setCsrfRequestAttributeName(null);
        return handler;
    }

    /**
     * The application is served from the same origin as this API, so the browser
     * needs none of this. It is kept for other web clients the API may serve.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
