package com.company.userservice.domain.exception;

public class InsufficientPermissionException extends RuntimeException {
    public InsufficientPermissionException(String message) {
        super(message);
    }
}
