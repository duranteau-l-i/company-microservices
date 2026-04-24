package com.company.officerservice.domain.port.usecases;

import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.Role;

import java.util.UUID;

public interface DeleteOfficerUseCase {
    void delete(Command command);

    record Command(UUID callerId, Role callerRole, OfficerId officerId) {}
}
