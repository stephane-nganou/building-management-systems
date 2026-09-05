package com.bms.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.stereotype.Component;

/**
 * The browser's counterpart to {@link KeycloakJwtAuthenticationConverter}: adds
 * the realm roles to the authorities of a session established by the
 * authorization code flow.
 *
 * <p>The roles are read from the ID token, which carries them because the realm
 * export gives the client a realm role mapper with {@code id.token.claim} set.
 * Keycloak leaves that off by default, so without it a signed in browser would
 * hold no role at all and every owner would look like an assistant.
 */
@Component
public class KeycloakAuthoritiesMapper implements GrantedAuthoritiesMapper {

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(
            Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> mapped = new HashSet<>(authorities);
        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OidcUserAuthority oidc) {
                mapped.addAll(RealmRoles.from(oidc.getIdToken().getClaims()));
            }
        }
        return mapped;
    }
}
