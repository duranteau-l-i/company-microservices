package com.company.officerservice.domain.port.usecases;

import com.company.officerservice.domain.model.OfficerRestrictedView;
import com.company.officerservice.domain.model.Role;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SearchOfficersUseCase {
    List<OfficerRestrictedView> search(Command command);

    record Command(UUID callerId, Role callerRole, String firstName, String lastName, LocalDate dateOfBirth) {}
}
