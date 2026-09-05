package com.bms.user;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the authenticated Keycloak principal to a local {@link AppUser}.
 *
 * <p>Reads claims rather than a particular kind of token, because a caller can
 * arrive two ways: a browser holding a session this service established, whose
 * principal is an {@link OidcUser}, or any other client presenting its own
 * access token, whose principal is a {@code Jwt}. Both are
 * {@link ClaimAccessor}s over the same Keycloak claims, and everything below
 * this point should not care which one it got.
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
        currentClaims().ifPresent(claims -> {
            String keycloakId = subjectOf(claims);
            users.findByKeycloakId(keycloakId)
                    .ifPresentOrElse(
                            user -> syncProfile(user, claims),
                            () -> users.save(new AppUser(
                                    keycloakId,
                                    email(claims),
                                    claims.getClaimAsString("given_name"),
                                    claims.getClaimAsString("family_name"))));
        });
    }

    @Transactional(readOnly = true)
    public AppUser require() {
        ClaimAccessor claims = currentClaims().orElseThrow(
                () -> new IllegalStateException("No authenticated principal is available"));
        return users.findByKeycloakId(subjectOf(claims))
                .orElseThrow(() -> new IllegalStateException("The signed in user has no local record"));
    }

    @Transactional(readOnly = true)
    public UUID requireId() {
        return require().getId();
    }

    private void syncProfile(AppUser user, ClaimAccessor claims) {
        String email = email(claims);
        String firstName = claims.getClaimAsString("given_name");
        String lastName = claims.getClaimAsString("family_name");
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
    private String subjectOf(ClaimAccessor claims) {
        String subject = claims.getClaimAsString("sub");
        if (subject == null) {
            throw new IllegalStateException(
                    "The token carries no 'sub' claim; add the 'basic' client scope in Keycloak");
        }
        return subject;
    }

    private String email(ClaimAccessor claims) {
        String email = claims.getClaimAsString("email");
        return email != null ? email : subjectOf(claims);
    }

    /** The claims of whichever credential the caller presented, if any. */
    private Optional<ClaimAccessor> currentClaims() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return Optional.of(token.getToken());
        }
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser user) {
            return Optional.of(user.getIdToken());
        }
        return Optional.empty();
    }
}
