package com.bms.identity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bms.common.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Lets one client sign up only so often. Registration needs no account, so
 * without this anyone could script it: to learn which emails have an account,
 * or to start trial after trial.
 *
 * <p>Counts per client address, in fixed windows of an hour. Behind Caddy that
 * address is the real client's, because the backend trusts the forwarded headers
 * Caddy sets. The counts live in memory, which is enough for the single backend
 * we run; a second one would need them shared.
 */
@Component
public class RegistrationRateLimit implements HandlerInterceptor {

    private static final Duration WINDOW = Duration.ofHours(1);
    private static final int PURGE_ABOVE = 10_000;

    private final int maxPerWindow;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RegistrationRateLimit(@Value("${bms.registration.max-per-hour}") int maxPerWindow, Clock clock) {
        this.maxPerWindow = maxPerWindow;
        this.clock = clock;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equals(request.getMethod())) {
            return true;
        }
        Instant now = clock.instant();
        if (windows.size() > PURGE_ABOVE) {
            windows.values().removeIf(window -> window.endedBy(now));
        }
        Window window = windows.compute(request.getRemoteAddr(),
                (address, current) -> current == null || current.endedBy(now) ? new Window(now, 1) : current.next());
        if (window.count() > maxPerWindow) {
            throw new TooManyRequestsException("error.registration.tooMany");
        }
        return true;
    }

    private record Window(Instant start, int count) {

        boolean endedBy(Instant now) {
            return !now.isBefore(start.plus(WINDOW));
        }

        Window next() {
            return new Window(start, count + 1);
        }
    }
}
