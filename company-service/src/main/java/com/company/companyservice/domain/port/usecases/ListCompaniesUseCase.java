package com.company.companyservice.domain.port.usecases;

import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.Role;

import java.util.List;
import java.util.UUID;

public interface ListCompaniesUseCase {
    List<CompanyFullView> list(Query query);

    record Query(UUID callerId, Role callerRole) {}
}
