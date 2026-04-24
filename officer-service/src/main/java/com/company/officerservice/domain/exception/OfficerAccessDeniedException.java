package com.company.officerservice.domain.exception;

public class OfficerAccessDeniedException extends RuntimeException {
    public OfficerAccessDeniedException(String message) {
        super(message);
    }
}
