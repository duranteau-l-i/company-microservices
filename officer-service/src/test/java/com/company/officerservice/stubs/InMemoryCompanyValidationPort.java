package com.company.officerservice.stubs;

import com.company.officerservice.domain.exception.ServiceUnavailableException;
import com.company.officerservice.domain.port.infrastructure.CompanyValidationPort;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InMemoryCompanyValidationPort implements CompanyValidationPort {

    private final Set<UUID> existingCompanies = new HashSet<>();
    private boolean simulateUnavailable = false;

    public void addCompany(UUID companyId) {
        existingCompanies.add(companyId);
    }

    public void setSimulateUnavailable(boolean simulateUnavailable) {
        this.simulateUnavailable = simulateUnavailable;
    }

    public void clear() {
        existingCompanies.clear();
        simulateUnavailable = false;
    }

    @Override
    public boolean companyExists(UUID companyId) {
        if (simulateUnavailable) {
            throw new ServiceUnavailableException("Cannot verify company — try again later");
        }
        return existingCompanies.contains(companyId);
    }
}
