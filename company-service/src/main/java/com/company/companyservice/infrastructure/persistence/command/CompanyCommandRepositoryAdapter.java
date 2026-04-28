package com.company.companyservice.infrastructure.persistence.command;

import com.company.companyservice.domain.model.Company;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.port.infrastructure.CompanyCommandRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class CompanyCommandRepositoryAdapter implements CompanyCommandRepository {

    private final CompanyEntityRepository jpa;

    public CompanyCommandRepositoryAdapter(CompanyEntityRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Company save(Company company) {
        CompanyEntity saved = jpa.save(CompanyEntityMapper.toEntity(company));
        return CompanyEntityMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Company> findById(CompanyId id) {
        return jpa.findById(id.value()).map(CompanyEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByRegistrationNumber(String registrationNumber) {
        return jpa.existsByRegistrationNumber(registrationNumber);
    }

    @Override
    @Transactional
    public void delete(CompanyId id) {
        jpa.deleteById(id.value());
    }
}
