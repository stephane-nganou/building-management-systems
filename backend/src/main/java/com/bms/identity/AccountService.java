package com.bms.identity;

import java.util.UUID;

import com.bms.common.exception.ValidationException;
import com.bms.subscription.SubscriptionService;
import com.bms.user.AccountRole;
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
    private final SubscriptionService subscriptions;

    public AccountService(KeycloakAdminClient keycloak, AppUserRepository users,
                          SubscriptionService subscriptions) {
        this.keycloak = keycloak;
        this.users = users;
        this.subscriptions = subscriptions;
    }

    /** Registers someone who manages their own buildings, on a trial. They chose their password. */
    @Transactional
    public AppUser createOwner(String email, String firstName, String lastName, String password) {
        return owner(create(email, firstName, lastName, password, AccountRole.OWNER, false));
    }

    /** An administrator signs an owner up, with a password to hand over like an assistant's. */
    @Transactional
    public NewAccount createOwnerWithGeneratedPassword(String email, String firstName, String lastName) {
        String password = GeneratedPassword.next();
        return new NewAccount(owner(create(email, firstName, lastName, password, AccountRole.OWNER, true)), password);
    }

    /** Keycloak first, so a refusal there leaves our record as it was. */
    @Transactional
    public void setSuspended(AppUser user, boolean suspended) {
        keycloak.setEnabled(user.getKeycloakId(), !suspended);
        if (suspended) {
            user.suspend();
        } else {
            user.reactivate();
        }
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
    public NewAccount createAssistant(String email, String firstName, String lastName, UUID createdByOwnerId) {
        String password = GeneratedPassword.next();
        AppUser assistant = create(email, firstName, lastName, password, AccountRole.ASSISTANT, true);
        assistant.createdBy(createdByOwnerId);
        return new NewAccount(assistant, password);
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
    public void changePassword(UUID userId, String currentPassword, String password) {
        AppUser user = users.findById(userId).orElseThrow(
                () -> new IllegalStateException("The signed in user has no local record"));
        keycloak.verifyPassword(user.getEmail(), currentPassword);
        keycloak.resetPassword(user.getKeycloakId(), password);
        user.passwordChosen();
    }

    private AppUser create(String email, String firstName, String lastName, String password,
                           AccountRole role, boolean mustChangePassword) {
        users.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw new ValidationException("error.account.exists", email);
        });
        String realmRole = role == AccountRole.ASSISTANT ? ASSISTANT_ROLE : OWNER_ROLE;
        // A password handed over means someone who knows the holder set the
        // account up and typed the address. One chosen by its holder came through
        // public registration, and Keycloak asks them to confirm the address.
        boolean emailVerified = mustChangePassword;
        String keycloakId = keycloak.createUser(email, firstName, lastName, password, realmRole, emailVerified);
        return users.save(new AppUser(keycloakId, email, firstName, lastName, role, mustChangePassword));
    }

    private AppUser owner(AppUser user) {
        subscriptions.startTrial(user);
        return user;
    }

    /** An account and the password to hand over, which is never readable again. */
    public record NewAccount(AppUser user, String password) {
    }
}
