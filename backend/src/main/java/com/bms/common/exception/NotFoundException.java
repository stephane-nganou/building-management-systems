package com.bms.common.exception;

import java.util.UUID;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String entity, UUID id) {
        return new NotFoundException(entity + " " + id + " was not found");
    }
}
