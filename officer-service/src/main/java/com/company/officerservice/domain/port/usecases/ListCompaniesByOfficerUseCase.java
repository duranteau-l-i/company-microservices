package com.company.officerservice.domain.port.usecases;

import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.OfficerView;
import com.company.officerservice.domain.model.Role;

import java.util.UUID;

public interface ListCompaniesByOfficerUseCase {
    OfficerView list(Command command);

    record Command(UUID callerId, Role callerRole, OfficerId officerId) {}
}
