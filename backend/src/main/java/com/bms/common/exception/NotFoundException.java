package com.bms.common.exception;

import java.util.UUID;

public class NotFoundException extends LocalizedException {

    public NotFoundException(String code, Object... args) {
        super(code, args);
    }

    /** The code names the entity, because French does not put the words in our order. */
    public static NotFoundException of(String code, UUID id) {
        return new NotFoundException(code, id);
    }
}
