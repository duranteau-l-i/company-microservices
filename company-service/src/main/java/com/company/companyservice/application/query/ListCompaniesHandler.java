package com.company.companyservice.application.query;

import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.infrastructure.CompanyQueryRepository;
import com.company.companyservice.domain.port.usecases.ListCompaniesUseCase;

import java.util.List;

public class ListCompaniesHandler implements ListCompaniesUseCase {

    private final CompanyQueryRepository queryRepo;

    public ListCompaniesHandler(CompanyQueryRepository queryRepo) {
        this.queryRepo = queryRepo;
    }

    @Override
    public List<CompanyFullView> list(Query query) {
        if (query.callerRole().isAtLeast(Role.MANAGER)) {
            return queryRepo.findAllFull();
        }
        return queryRepo.findFullByOwnerId(query.callerId());
    }
}
