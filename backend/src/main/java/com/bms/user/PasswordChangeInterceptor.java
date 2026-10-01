package com.bms.user;

import com.bms.common.exception.AccessDeniedForResourceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Refuses an account still holding the password it was handed everything but
 * choosing its own. The application confines such an account to its password
 * screen; this is what holds for any other client of the API.
 */
@Component
public class PasswordChangeInterceptor implements HandlerInterceptor {

    private final CurrentUserService currentUser;

    public PasswordChangeInterceptor(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (currentUser.mustChangePassword()) {
            throw new AccessDeniedForResourceException("error.password.mustChange");
        }
        return true;
    }
}
