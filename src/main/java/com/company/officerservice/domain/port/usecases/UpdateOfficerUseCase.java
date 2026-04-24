package com.company.officerservice.domain.port.usecases;

import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.Role;

import java.time.LocalDate;
import java.util.UUID;

public interface UpdateOfficerUseCase {
    OfficerFullView update(Command command);

    record Command(
            UUID callerId,
            Role callerRole,
            OfficerId officerId,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String nationality,
            String street,
            String city,
            String postalCode,
            String country,
            String email,
            String phone
    ) {}
}
