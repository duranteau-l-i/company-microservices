package com.company.companyservice.domain.port.infrastructure;

import com.company.companyservice.domain.model.Company;
import com.company.companyservice.domain.model.CompanyId;

import java.util.Optional;

public interface CompanyCommandRepository {
    Company save(Company company);
    Optional<Company> findById(CompanyId id);
    boolean existsByRegistrationNumber(String registrationNumber);
    void delete(CompanyId id);
}
