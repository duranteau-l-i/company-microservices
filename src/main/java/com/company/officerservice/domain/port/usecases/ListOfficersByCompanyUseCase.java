package com.company.officerservice.domain.port.usecases;

import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.Role;

import java.util.List;
import java.util.UUID;

public interface ListOfficersByCompanyUseCase {
    List<OfficerFullView> list(Command command);

    record Command(UUID callerId, Role callerRole, UUID companyId) {}
}
