package com.company.companyservice.domain.port.usecases;

import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.Role;

import java.util.UUID;

public interface DeleteCompanyUseCase {
    void delete(Command command);

    record Command(UUID callerId, Role callerRole, CompanyId companyId) {}
}
