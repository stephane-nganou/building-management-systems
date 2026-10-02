package com.bms.config;

import java.io.IOException;

import com.bms.metrics.ActivityRecorder;
import com.bms.user.CurrentUserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

/**
 * Hands the browser back to the application once Keycloak has vouched for it,
 * on the page it originally asked for, and counts the sign in.
 *
 * <p>Spring's own default sends the caller to the saved request, which here is
 * the API call that was refused rather than the screen the user was looking at.
 *
 * <p>The callback never reaches the filter that provisions a caller, so a first
 * sign in creates the local record here, before it can be counted.
 */
class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final FrontendProperties frontend;
    private final CurrentUserService currentUser;
    private final ActivityRecorder activity;

    LoginSuccessHandler(FrontendProperties frontend, CurrentUserService currentUser, ActivityRecorder activity) {
        this.frontend = frontend;
        this.currentUser = currentUser;
        this.activity = activity;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        currentUser.provisionCurrent().ifPresent(activity::signedIn);
        String path = LoginReturnPath.consume(request);
        String target = path == null ? frontend.homeUrl() : frontend.baseUrl() + path;
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
