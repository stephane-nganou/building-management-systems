package com.bms.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Notes where to come back to, on the way out to Keycloak.
 *
 * <p>Runs inside the security chain, ahead of the filter that builds the
 * authorization request: that one answers with a redirect and never continues
 * the chain, so anything registered after the chain would never see a sign in
 * request at all.
 */
class LoginReturnPathFilter extends OncePerRequestFilter {

    private final String authorizationBaseUri;

    LoginReturnPathFilter(String authorizationBaseUri) {
        this.authorizationBaseUri = authorizationBaseUri;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LoginReturnPath.remember(request);
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(authorizationBaseUri);
    }
}
