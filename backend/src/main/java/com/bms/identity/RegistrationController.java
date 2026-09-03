package com.bms.identity;

import com.bms.identity.dto.RegistrationRequest;
import com.bms.identity.dto.RegistrationResponse;
import com.bms.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The only endpoint reachable without a token: signing up as an owner. */
@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private final AccountService accounts;

    public RegistrationController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody RegistrationRequest request) {
        AppUser user = accounts.createOwner(
                request.email(), request.firstName(), request.lastName(), request.password());
        return new RegistrationResponse(user.getId(), user.getEmail(), user.getFullName());
    }
}
