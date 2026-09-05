package com.bms.identity;

import com.bms.identity.dto.PasswordChangeRequest;
import com.bms.identity.dto.RegistrationRequest;
import com.bms.identity.dto.RegistrationResponse;
import com.bms.user.AppUser;
import com.bms.user.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * What is left of authentication once Keycloak sits behind this service.
 *
 * <p>Signing in and out are not here: they are the authorization code flow, run
 * by Spring Security at {@code /api/auth/login/keycloak} and
 * {@code /api/auth/logout}. What remains is the two things Keycloak will not do
 * on our terms, creating an owner and replacing a password that was handed over.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accounts;
    private final CurrentUserService currentUser;

    public AuthController(AccountService accounts, CurrentUserService currentUser) {
        this.accounts = accounts;
        this.currentUser = currentUser;
    }

    /** The only endpoint reachable without signing in: signing up as an owner. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody RegistrationRequest request) {
        AppUser user = accounts.createOwner(
                request.email(), request.firstName(), request.lastName(), request.password());
        return new RegistrationResponse(user.getId(), user.getEmail(), user.getFullName());
    }

    /** Replaces the caller's password, and clears any obligation to do so. */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        accounts.changePassword(currentUser.requireId(), request.newPassword());
    }
}
