package com.company.officerservice.infrastructure.persistence.command;

import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.port.infrastructure.OfficerCommandRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresOfficerCommandRepository implements OfficerCommandRepository {

    private final OfficerJpaRepository jpa;
    private final EntityManager em;

    public PostgresOfficerCommandRepository(OfficerJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    @Transactional
    public Officer save(Officer officer) {
        UUID id = officer.id().value();
        // Explicitly delete existing links before re-inserting to avoid unique constraint
        // violations caused by Hibernate's INSERT-before-DELETE ordering with orphanRemoval.
        if (jpa.existsById(id)) {
            em.createQuery("DELETE FROM CompanyLinkJpaEntity l WHERE l.officer.id = :id")
                    .setParameter("id", id)
                    .executeUpdate();
        }
        OfficerJpaEntity saved = jpa.save(OfficerJpaMapper.toEntity(officer));
        return OfficerJpaMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Officer> findById(OfficerId id) {
        return jpa.findById(id.value()).map(OfficerJpaMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Officer> findByNameAndDateOfBirth(String firstName, String lastName, LocalDate dateOfBirth) {
        return jpa.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(firstName, lastName, dateOfBirth)
                .stream()
                .map(OfficerJpaMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void delete(OfficerId id) {
        jpa.deleteById(id.value());
    }
}
