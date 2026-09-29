package com.bms.config;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Reads Keycloak's realm roles out of a token's claims.
 *
 * <p>Shared by both ways in which a caller can arrive: a mobile client presents
 * an access token, a browser presents a session cookie backed by an ID token.
 * The roles decide the same things either way, so they are read the same way.
 */
final class RealmRoles {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";

    private RealmRoles() {
    }

    static Set<GrantedAuthority> from(Map<String, Object> claims) {
        Object realmAccess = claims.get(REALM_ACCESS);
        if (!(realmAccess instanceof Map<?, ?> access) || !(access.get(ROLES) instanceof List<?> roles)) {
            return Set.of();
        }
        return roles.stream()
                .map(String::valueOf)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toSet());
    }
}
