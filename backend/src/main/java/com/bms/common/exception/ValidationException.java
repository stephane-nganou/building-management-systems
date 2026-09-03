package com.bms.common.exception;

/** Raised when a request is syntactically valid but breaks a business rule. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
