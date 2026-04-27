package com.company.officerservice.stubs;

import com.company.officerservice.domain.port.infrastructure.CompanyValidationPort;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InMemoryCompanyValidationPort implements CompanyValidationPort {

    private final Set<UUID> existingCompanies = new HashSet<>();

    public void addCompany(UUID companyId) {
        existingCompanies.add(companyId);
    }

    public void clear() {
        existingCompanies.clear();
    }

    @Override
    public boolean companyExists(UUID companyId) {
        return existingCompanies.contains(companyId);
    }
}
