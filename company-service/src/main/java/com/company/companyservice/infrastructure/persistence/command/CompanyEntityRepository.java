package com.company.companyservice.infrastructure.persistence.command;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyEntityRepository extends JpaRepository<CompanyEntity, UUID> {
    boolean existsByRegistrationNumber(String registrationNumber);
}
