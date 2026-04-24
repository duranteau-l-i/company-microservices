package com.company.companyservice.domain.exception;

public class CompanyAccessDeniedException extends RuntimeException {
    public CompanyAccessDeniedException(String message) {
        super(message);
    }
}
