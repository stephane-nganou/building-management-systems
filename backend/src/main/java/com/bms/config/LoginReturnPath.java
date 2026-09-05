package com.bms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * The page the browser asked for before it was sent off to sign in, kept in the
 * session across the round trip to Keycloak.
 *
 * <p>Only a path is ever kept, never a whole URL, and the success handler joins
 * it to the configured frontend address. An attacker who could put a URL here
 * would have turned our sign in link into a redirect to their own site; a path
 * that must begin with a single slash cannot leave the application.
 */
final class LoginReturnPath {

    private static final String ATTRIBUTE = "bms.login.returnPath";

    private LoginReturnPath() {
    }

    /** Stores the {@code redirect} parameter of a sign in request, if it is usable. */
    static void remember(HttpServletRequest request) {
        String path = sanitize(request.getParameter("redirect"));
        if (path != null) {
            request.getSession(true).setAttribute(ATTRIBUTE, path);
        }
    }

    /** Reads the remembered path and forgets it, so a later sign in starts clean. */
    static String consume(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object path = session.getAttribute(ATTRIBUTE);
        session.removeAttribute(ATTRIBUTE);
        return path instanceof String value ? value : null;
    }

    /**
     * A path within the application, or nothing.
     *
     * <p>A second slash or a backslash is rejected because a browser reads
     * {@code //evil.test} and {@code /\evil.test} as protocol relative URLs
     * pointing at another host.
     */
    private static String sanitize(String candidate) {
        if (candidate == null || candidate.length() < 1 || candidate.charAt(0) != '/') {
            return null;
        }
        if (candidate.length() > 1 && (candidate.charAt(1) == '/' || candidate.charAt(1) == '\\')) {
            return null;
        }
        return candidate;
    }
}
