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
     * Someone is an assistant or an administrator only when the realm says so and
     * says nothing about owning. Anything else is treated as an owner, which is
     * what a user who signed up before roles existed still is.
     */
    public static boolean isOwner(Authentication authentication) {
        Set<String> authorities = authorities(authentication);
        return authorities.contains(OWNER)
                || !(authorities.contains(ASSISTANT) || authorities.contains(ADMIN));
    }

    private static Set<String> authorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
