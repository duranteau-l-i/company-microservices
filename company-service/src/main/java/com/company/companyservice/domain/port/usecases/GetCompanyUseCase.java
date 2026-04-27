package com.company.companyservice.domain.port.usecases;

import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyView;
import com.company.companyservice.domain.model.Role;

import java.util.UUID;

public interface GetCompanyUseCase {
    Result get(Query query);

    record Query(UUID callerId, Role callerRole, CompanyId companyId) {}

    record Result(CompanyView view) {}
}
