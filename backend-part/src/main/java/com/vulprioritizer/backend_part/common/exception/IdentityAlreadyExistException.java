package com.vulprioritizer.backend_part.common.exception;

public class IdentityAlreadyExistException extends RuntimeException {
    public IdentityAlreadyExistException(String message) {
        super(message);
    }
}
