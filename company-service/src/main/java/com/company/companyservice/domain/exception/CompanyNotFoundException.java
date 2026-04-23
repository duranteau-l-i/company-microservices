package com.company.companyservice.domain.exception;

import com.company.companyservice.domain.model.CompanyId;

public class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException(CompanyId id) {
        super("Company not found: " + id);
    }
}
