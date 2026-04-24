package com.company.officerservice.domain.port.infrastructure;

import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.OfficerId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OfficerCommandRepository {
    Officer save(Officer officer);
    Optional<Officer> findById(OfficerId id);
    List<Officer> findByNameAndDateOfBirth(String firstName, String lastName, LocalDate dateOfBirth);
    void delete(OfficerId id);
}
