package com.bms.user;

import java.io.IOException;

import com.bms.metrics.ActivityRecorder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every authenticated caller a local record before the request reaches a
 * controller, so read only request paths never have to create one, and notes
 * that they were active today.
 */
@Component
public class UserProvisioningFilter extends OncePerRequestFilter {

    private final CurrentUserService currentUser;
    private final ActivityRecorder activity;

    public UserProvisioningFilter(CurrentUserService currentUser, ActivityRecorder activity) {
        this.currentUser = currentUser;
        this.activity = activity;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        currentUser.provisionCurrent().ifPresent(activity::touch);
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
}
