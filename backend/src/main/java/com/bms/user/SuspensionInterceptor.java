package com.bms.user;

import com.bms.common.exception.AccessDeniedForResourceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Refuses a suspended account everything, a browser session opened before the
 * suspension included; disabling the account in Keycloak only stops new sign
 * ins. An interceptor rather than a filter, so the refusal is translated and
 * shaped like every other error.
 */
@Component
public class SuspensionInterceptor implements HandlerInterceptor {

    private final CurrentUserService currentUser;

    public SuspensionInterceptor(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (currentUser.isSuspended()) {
            throw new AccessDeniedForResourceException("error.account.suspended");
        }
        return true;
    }
}
