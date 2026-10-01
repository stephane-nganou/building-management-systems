package com.bms.user;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** What the realm roles on a caller make them. */
public final class Roles {

    private static final String OWNER = "ROLE_OWNER";
    private static final String ASSISTANT = "ROLE_ASSISTANT";
    private static final String ADMIN = "ROLE_ADMIN";

    private Roles() {
    }

    public static boolean isAdmin(Authentication authentication) {
        return authorities(authentication).contains(ADMIN);
    }

    /**
     * Only the realm's owner role makes an owner. An account with no role at all,
     * such as one made by hand in Keycloak, or the backend's own service account,
     * owns nothing and gets no trial.
     */
    public static boolean isOwner(Authentication authentication) {
        return authorities(authentication).contains(OWNER);
    }

    public static boolean isAssistant(Authentication authentication) {
        return authorities(authentication).contains(ASSISTANT);
    }

    private static Set<String> authorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
