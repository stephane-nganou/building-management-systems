package com.bms.common.exception;

public class AccessDeniedForResourceException extends LocalizedException {

    public AccessDeniedForResourceException(String code, Object... args) {
        super(code, args);
    }
}
