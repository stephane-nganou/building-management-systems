package com.bms.user;

import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the authenticated Keycloak principal to a local {@link AppUser},
 * creating the local record the first time a user is seen.
 */
@Service
public class CurrentUserService {

    private final AppUserRepository users;

    public CurrentUserService(AppUserRepository users) {
        this.users = users;
    }

    @Transactional
    public AppUser require() {
        Jwt jwt = currentJwt();
        String keycloakId = jwt.getSubject();
        return users.findByKeycloakId(keycloakId)
                .map(user -> syncProfile(user, jwt))
                .orElseGet(() -> users.save(new AppUser(
                        keycloakId,
                        email(jwt),
                        jwt.getClaimAsString("given_name"),
                        jwt.getClaimAsString("family_name"))));
    }

    @Transactional
    public UUID requireId() {
        return require().getId();
    }

    private AppUser syncProfile(AppUser user, Jwt jwt) {
        String email = email(jwt);
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");
        if (!email.equals(user.getEmail())
                || !Objects.equals(firstName, user.getFirstName())
                || !Objects.equals(lastName, user.getLastName())) {
            user.updateProfile(email, firstName, lastName);
        }
        return user;
    }

    private String email(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return email != null ? email : jwt.getSubject();
    }

    private Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        throw new IllegalStateException("No authenticated JWT principal is available");
    }
}
