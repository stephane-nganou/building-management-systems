package com.bms.config;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

/**
 * Hands the browser back to the application once Keycloak has vouched for it,
 * on the page it originally asked for.
 *
 * <p>Spring's own default sends the caller to the saved request, which here is
 * the API call that was refused rather than the screen the user was looking at.
 */
class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final FrontendProperties frontend;

    LoginSuccessHandler(FrontendProperties frontend) {
        this.frontend = frontend;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String path = LoginReturnPath.consume(request);
        String target = path == null ? frontend.homeUrl() : frontend.baseUrl() + path;
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
