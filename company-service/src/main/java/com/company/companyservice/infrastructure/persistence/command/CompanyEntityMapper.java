package com.company.companyservice.infrastructure.persistence.command;

import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.Company;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyStatus;

public final class CompanyEntityMapper {

    private CompanyEntityMapper() {}

    public static CompanyEntity toEntity(Company company) {
        return new CompanyEntity(
                company.id().value(),
                company.name(),
                company.registrationNumber(),
                company.address().street(),
                company.address().city(),
                company.address().postalCode(),
                company.address().country(),
                company.ownerId(),
                company.status().name(),
                company.createdAt(),
                company.updatedAt()
        );
    }

    public static Company toDomain(CompanyEntity entity) {
        return new Company(
                CompanyId.of(entity.getId()),
                entity.getName(),
                entity.getRegistrationNumber(),
                new Address(entity.getStreet(), entity.getCity(), entity.getPostalCode(), entity.getCountry()),
                entity.getOwnerId(),
                CompanyStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
