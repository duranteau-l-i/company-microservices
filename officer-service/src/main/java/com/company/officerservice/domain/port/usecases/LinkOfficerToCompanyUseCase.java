package com.company.officerservice.domain.port.usecases;

import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.Role;

import java.time.LocalDate;
import java.util.UUID;

public interface LinkOfficerToCompanyUseCase {
    OfficerFullView link(Command command);

    record Command(
            UUID callerId,
            Role callerRole,
            OfficerId officerId,
            UUID companyId,
            String title,
            LocalDate appointmentDate
    ) {}
}
