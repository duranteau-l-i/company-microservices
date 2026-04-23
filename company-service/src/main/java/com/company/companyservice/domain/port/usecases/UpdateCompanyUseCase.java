package com.company.companyservice.domain.port.usecases;

import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.Role;

import java.util.UUID;

public interface UpdateCompanyUseCase {
    CompanyFullView update(Command command);

    record Command(
            UUID callerId,
            Role callerRole,
            CompanyId companyId,
            String name,
            String registrationNumber,
            String street,
            String city,
            String postalCode,
            String country
    ) {}
}
