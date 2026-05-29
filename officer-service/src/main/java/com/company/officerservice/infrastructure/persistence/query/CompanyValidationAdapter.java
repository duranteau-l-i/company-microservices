package com.company.officerservice.infrastructure.persistence.query;

import com.company.officerservice.domain.port.infrastructure.CompanyValidationPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CompanyValidationAdapter implements CompanyValidationPort {

    private final KnownCompanyRepository repository;

    public CompanyValidationAdapter(KnownCompanyRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean companyExists(UUID companyId) {
        return repository.existsById(companyId);
    }

    @Override
    public Optional<UUID> findOwnerId(UUID companyId) {
        return repository.findById(companyId).map(KnownCompanyDocument::getOwnerId);
    }
}
