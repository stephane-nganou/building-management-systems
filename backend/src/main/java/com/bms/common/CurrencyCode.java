package com.bms.common;

import java.util.Currency;

/**
 * The currencies a building can keep its books in. A closed list rather than
 * any ISO 4217 code, so every one of them is known to render correctly on
 * screen and on an invoice. Adding one is a constant here and its twin in the
 * frontend's list.
 */
public enum CurrencyCode {
    EUR, XAF, XOF, USD, GBP, CHF;

    /** Digits after the decimal point: two for the euro, none for the CFA francs. */
    public int fractionDigits() {
        return Currency.getInstance(name()).getDefaultFractionDigits();
    }
}
