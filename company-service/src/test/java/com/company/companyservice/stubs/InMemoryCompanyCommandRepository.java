package com.company.companyservice.stubs;

import com.company.companyservice.domain.model.Company;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.port.infrastructure.CompanyCommandRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryCompanyCommandRepository implements CompanyCommandRepository {

    private final Map<CompanyId, Company> store = new HashMap<>();

    @Override
    public Company save(Company company) {
        store.put(company.id(), company);
        return company;
    }

    @Override
    public Optional<Company> findById(CompanyId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsByRegistrationNumber(String registrationNumber) {
        return store.values().stream()
                .anyMatch(c -> c.registrationNumber().equals(registrationNumber));
    }

    @Override
    public void delete(CompanyId id) {
        store.remove(id);
    }

    public void clear() {
        store.clear();
    }

    public int size() {
        return store.size();
    }
}
