package com.company.officerservice.infrastructure.persistence.command;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OfficerJpaRepository extends JpaRepository<OfficerJpaEntity, UUID> {
    List<OfficerJpaEntity> findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(
            String firstName, String lastName, LocalDate dateOfBirth);
}
