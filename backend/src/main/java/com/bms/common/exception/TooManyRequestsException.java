package com.bms.common.exception;

/** The caller asked too often, and should try again later. */
public class TooManyRequestsException extends LocalizedException {

    public TooManyRequestsException(String code, Object... args) {
        super(code, args);
    }
}
