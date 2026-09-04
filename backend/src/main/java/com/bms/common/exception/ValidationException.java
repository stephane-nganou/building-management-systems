package com.bms.common.exception;

/** Raised when a request is syntactically valid but breaks a business rule. */
public class ValidationException extends LocalizedException {

    public ValidationException(String code, Object... args) {
        super(code, args);
    }
}
