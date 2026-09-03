package com.bms.user;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the authenticated Keycloak principal to a local {@link AppUser}.
 *
 * <p>Creating the local record is deliberately separate from reading it, and runs
 * from {@link UserProvisioningFilter} before any request scoped transaction opens.
 * Provisioning lazily inside a service would silently do nothing whenever the
 * caller's outermost transaction is read only, because Hibernate does not flush
 * there.
 */
@Service
public class CurrentUserService {

    private final AppUserRepository users;

    public CurrentUserService(AppUserRepository users) {
        this.users = users;
    }

    /** Creates or refreshes the local record for the caller. Runs read write. */
    @Transactional
    public void provisionCurrent() {
        currentJwt().ifPresent(jwt -> {
            String keycloakId = subjectOf(jwt);
            users.findByKeycloakId(keycloakId)
                    .ifPresentOrElse(
                            user -> syncProfile(user, jwt),
                            () -> users.save(new AppUser(
                                    keycloakId,
                                    email(jwt),
                                    jwt.getClaimAsString("given_name"),
                                    jwt.getClaimAsString("family_name"))));
        });
    }

    @Transactional(readOnly = true)
    public AppUser require() {
        Jwt jwt = currentJwt().orElseThrow(
                () -> new IllegalStateException("No authenticated JWT principal is available"));
        return users.findByKeycloakId(subjectOf(jwt))
                .orElseThrow(() -> new IllegalStateException("The signed in user has no local record"));
    }

    @Transactional(readOnly = true)
    public UUID requireId() {
        return require().getId();
    }

    private void syncProfile(AppUser user, Jwt jwt) {
        String email = email(jwt);
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");
        if (!email.equals(user.getEmail())
                || !Objects.equals(firstName, user.getFirstName())
                || !Objects.equals(lastName, user.getLastName())) {
            user.updateProfile(email, firstName, lastName);
        }
    }

    /**
     * The subject is the only stable identifier we have. Keycloak only emits it
     * when the client carries the "basic" scope, so a missing value is a
     * configuration problem worth naming precisely.
     */
    private String subjectOf(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null) {
            throw new IllegalStateException(
                    "The access token carries no 'sub' claim; add the 'basic' client scope in Keycloak");
        }
        return subject;
    }

    private String email(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return email != null ? email : subjectOf(jwt);
    }

    private Optional<Jwt> currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof JwtAuthenticationToken token
                ? Optional.of(token.getToken())
                : Optional.empty();
    }
}
