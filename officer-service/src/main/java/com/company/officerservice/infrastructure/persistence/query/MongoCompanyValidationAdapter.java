package com.company.officerservice.infrastructure.persistence.query;

import com.company.officerservice.domain.port.infrastructure.CompanyValidationPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MongoCompanyValidationAdapter implements CompanyValidationPort {

    private final KnownCompanyMongoRepository repository;

    public MongoCompanyValidationAdapter(KnownCompanyMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean companyExists(UUID companyId) {
        return repository.existsById(companyId);
    }
}
