package com.bms.subscription;

import com.bms.common.exception.AccessDeniedForResourceException;
import com.bms.subscription.dto.PeriodResponse;
import com.bms.subscription.dto.SubscriptionResponse;
import com.bms.user.AppUser;
import com.bms.user.CurrentUserService;
import com.bms.user.Roles;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * An owner's own view of their subscription. Both endpoints stay open to a
 * suspended account, which is refused everything else and most needs to know
 * whom to call.
 */
@RestController
public class SubscriptionController {

    private final SubscriptionService subscriptions;
    private final CurrentUserService currentUser;
    private final SupportContacts support;

    public SubscriptionController(SubscriptionService subscriptions, CurrentUserService currentUser,
                                  SupportContacts support) {
        this.subscriptions = subscriptions;
        this.currentUser = currentUser;
        this.support = support;
    }

    /** Owners only: an assistant works under someone else's subscription and manages none. */
    @GetMapping("/api/subscription")
    @Transactional(readOnly = true)
    public SubscriptionResponse subscription(Authentication authentication) {
        if (!Roles.isOwner(authentication)) {
            throw new AccessDeniedForResourceException("error.subscription.ownersOnly");
        }
        AppUser owner = currentUser.require();
        return new SubscriptionResponse(
                subscriptions.summary(owner),
                subscriptions.periods(owner).stream().map(PeriodResponse::from).toList(),
                support);
    }

    @GetMapping("/api/support")
    public SupportContacts support() {
        return support;
    }
}
