package com.company.officerservice.domain.exception;

public class OfficerNotFoundException extends RuntimeException {
    public OfficerNotFoundException(String id) {
        super("Officer not found: " + id);
    }
}
