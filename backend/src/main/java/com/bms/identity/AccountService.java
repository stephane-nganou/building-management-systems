package com.bms.identity;

import java.util.UUID;

import com.bms.common.exception.ValidationException;
import com.bms.user.AppUser;
import com.bms.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates accounts in Keycloak and mirrors them locally straight away, so an
 * owner can be granted work before their first sign in.
 */
@Service
public class AccountService {

    public static final String OWNER_ROLE = "owner";
    public static final String ASSISTANT_ROLE = "assistant";

    private final KeycloakAdminClient keycloak;
    private final AppUserRepository users;

    public AccountService(KeycloakAdminClient keycloak, AppUserRepository users) {
        this.keycloak = keycloak;
        this.users = users;
    }

    /** Registers someone who manages their own buildings. They chose their password. */
    @Transactional
    public AppUser createOwner(String email, String firstName, String lastName, String password) {
        return create(email, firstName, lastName, password, OWNER_ROLE, false);
    }

    /**
     * Creates an assistant on an owner's behalf, with a password to hand over.
     *
     * <p>That password is a normal one as far as Keycloak is concerned. Marking
     * it temporary there would make Keycloak demand a new one on its own page,
     * and the browser never goes there for anything but signing in. The
     * obligation is recorded on our own record instead, and
     * {@code POST /api/auth/password} discharges it.
     */
    @Transactional
    public NewAccount createAssistant(String email, String firstName, String lastName) {
        String password = GeneratedPassword.next();
        return new NewAccount(create(email, firstName, lastName, password, ASSISTANT_ROLE, true), password);
    }

    /** Issues a fresh password for an existing account, to be replaced in turn. */
    @Transactional
    public String resetPassword(AppUser user) {
        String password = GeneratedPassword.next();
        keycloak.resetPassword(user.getKeycloakId(), password);
        user.requirePasswordChange();
        return password;
    }

    /**
     * Sets the password its holder chose, and lets them get on with their work.
     *
     * <p>Takes an id rather than the record itself: the caller reads that in a
     * transaction of its own, which has ended by the time this one begins, and a
     * detached entity would take the change no further than memory.
     */
    @Transactional
    public void changePassword(UUID userId, String password) {
        AppUser user = users.findById(userId).orElseThrow(
                () -> new IllegalStateException("The signed in user has no local record"));
        keycloak.resetPassword(user.getKeycloakId(), password);
        user.passwordChosen();
    }

    private AppUser create(String email, String firstName, String lastName, String password,
                           String realmRole, boolean mustChangePassword) {
        users.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw new ValidationException("error.account.exists", email);
        });
        String keycloakId = keycloak.createUser(email, firstName, lastName, password, realmRole);
        return users.save(new AppUser(keycloakId, email, firstName, lastName, mustChangePassword));
    }

    /** An account and the password to hand over, which is never readable again. */
    public record NewAccount(AppUser user, String password) {
    }
}
