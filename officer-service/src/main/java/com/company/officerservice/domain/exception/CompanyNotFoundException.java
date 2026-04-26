package com.company.officerservice.domain.exception;

import java.util.UUID;

public class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException(UUID companyId) {
        super("Company not found: " + companyId);
    }
}
