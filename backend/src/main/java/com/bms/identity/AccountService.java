package com.bms.identity;

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

    /** Registers someone who manages their own buildings. They choose their password. */
    @Transactional
    public AppUser createOwner(String email, String firstName, String lastName, String password) {
        return create(email, firstName, lastName, password, false, OWNER_ROLE);
    }

    /**
     * Creates an assistant on an owner's behalf. The generated password is
     * temporary, so Keycloak makes them choose their own at first sign in.
     */
    @Transactional
    public NewAccount createAssistant(String email, String firstName, String lastName) {
        String password = GeneratedPassword.next();
        return new NewAccount(create(email, firstName, lastName, password, true, ASSISTANT_ROLE), password);
    }

    /** Issues a fresh temporary password for an existing account. */
    public String resetPassword(AppUser user) {
        String password = GeneratedPassword.next();
        keycloak.resetPassword(user.getKeycloakId(), password, true);
        return password;
    }

    private AppUser create(String email, String firstName, String lastName, String password,
                           boolean temporaryPassword, String realmRole) {
        users.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw new ValidationException("error.account.exists", email);
        });
        String keycloakId = keycloak.createUser(email, firstName, lastName, password, temporaryPassword, realmRole);
        return users.save(new AppUser(keycloakId, email, firstName, lastName));
    }

    /** An account and the password to hand over, which is never readable again. */
    public record NewAccount(AppUser user, String password) {
    }
}
