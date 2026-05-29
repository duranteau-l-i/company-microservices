package com.company.officerservice.stubs;

import com.company.officerservice.domain.port.infrastructure.CompanyValidationPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryCompanyValidationPort implements CompanyValidationPort {

    private final Map<UUID, UUID> companyOwners = new HashMap<>();

    public void addCompany(UUID companyId, UUID ownerId) {
        companyOwners.put(companyId, ownerId);
    }

    public void clear() {
        companyOwners.clear();
    }

    @Override
    public boolean companyExists(UUID companyId) {
        return companyOwners.containsKey(companyId);
    }

    @Override
    public Optional<UUID> findOwnerId(UUID companyId) {
        return Optional.ofNullable(companyOwners.get(companyId));
    }
}
