package com.bms.metrics;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Times every API request and counts it under its route pattern.
 *
 * <p>It runs ahead of security, so refused requests are counted too. The route
 * is the pattern a controller declared, {@code /api/invoices/{id}/pdf}, never
 * the address itself: one route stays one row, and no identifier is kept. A
 * request no controller matched counts as {@code UNMATCHED}.
 *
 * <p>The method is whatever the caller wrote, before anybody has checked who
 * they are, so only the standard ones keep their name. Anything else is
 * {@code OTHER}: an invented method can neither overflow the column nor grow
 * the counts in memory without end.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestStatsFilter extends OncePerRequestFilter {

    private static final String UNMATCHED = "UNMATCHED";
    private static final String OTHER = "OTHER";
    private static final Set<String> METHODS =
            Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE");

    private final RequestStats stats;

    public RequestStatsFilter(RequestStats stats) {
        this.stats = stats;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        try {
            chain.doFilter(request, response);
            status = response.getStatus();
        } finally {
            Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String route = pattern == null ? UNMATCHED : pattern.toString();
            String method = METHODS.contains(request.getMethod()) ? request.getMethod() : OTHER;
            stats.record(new RequestStats.Route(method, route, status),
                    (System.nanoTime() - start) / 1_000_000);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
}
