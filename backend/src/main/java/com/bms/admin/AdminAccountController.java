package com.bms.admin;

import java.util.List;
import java.util.UUID;

import com.bms.admin.dto.AccountResponse;
import com.bms.admin.dto.OwnerRequest;
import com.bms.admin.dto.PeriodRequest;
import com.bms.admin.dto.PeriodResponse;
import com.bms.subscription.SubscriptionStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Administrators only; {@code SecurityConfig} refuses everyone else the whole of /api/admin. */
@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService accounts;

    public AdminAccountController(AdminAccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public List<AccountResponse> list(@RequestParam(required = false) SubscriptionStatus status) {
        return accounts.list(status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody OwnerRequest request) {
        return accounts.create(request);
    }

    @GetMapping("/{id}/periods")
    public List<PeriodResponse> periods(@PathVariable UUID id) {
        return accounts.periods(id);
    }

    @PostMapping("/{id}/periods")
    @ResponseStatus(HttpStatus.CREATED)
    public PeriodResponse addPeriod(@PathVariable UUID id, @Valid @RequestBody PeriodRequest request) {
        return accounts.addPeriod(id, request);
    }

    /** Ends the subscription today: yesterday becomes the last day of the current period. */
    @PostMapping("/{id}/periods/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void endCurrentPeriod(@PathVariable UUID id) {
        accounts.endCurrentPeriod(id);
    }

    @PostMapping("/{id}/suspension")
    public AccountResponse suspend(@PathVariable UUID id) {
        return accounts.setSuspended(id, true);
    }

    @DeleteMapping("/{id}/suspension")
    public AccountResponse reactivate(@PathVariable UUID id) {
        return accounts.setSuspended(id, false);
    }
}
