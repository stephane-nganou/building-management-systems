package com.bms.invoice;

import java.util.Set;

/**
 * Where an invoice is in its life, which only ever moves forward. Once sent, an
 * invoice is in the tenant's hands and in the books, so it never becomes a draft
 * again, which is the only state that can be deleted. Cancelling is the way to
 * withdraw it, and a paid one can still be cancelled, for a refund.
 */
public enum InvoiceStatus {
    DRAFT,
    SENT,
    PAID,
    CANCELLED;

    public boolean canBecome(InvoiceStatus target) {
        return switch (this) {
            case DRAFT -> Set.of(SENT, CANCELLED).contains(target);
            case SENT -> Set.of(PAID, CANCELLED).contains(target);
            case PAID -> target == CANCELLED;
            case CANCELLED -> false;
        };
    }
}
