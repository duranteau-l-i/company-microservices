package com.company.companyservice.application.query;

import com.company.companyservice.domain.exception.CompanyNotFoundException;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyView;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.infrastructure.CompanyQueryRepository;
import com.company.companyservice.domain.port.usecases.GetCompanyUseCase;

public class GetCompanyHandler implements GetCompanyUseCase {

    private final CompanyQueryRepository queryRepo;

    public GetCompanyHandler(CompanyQueryRepository queryRepo) {
        this.queryRepo = queryRepo;
    }

    @Override
    public CompanyView get(Query query) {
        CompanyFullView full = queryRepo.findFullById(query.companyId())
                .orElseThrow(() -> new CompanyNotFoundException(query.companyId()));

        boolean isOwner = full.ownerId().equals(query.callerId());
        boolean canSeeFull = query.callerRole().isAtLeast(Role.MANAGER) || isOwner;

        if (canSeeFull) {
            return full;
        }
        return queryRepo.findRestrictedById(query.companyId())
                .orElseThrow(() -> new CompanyNotFoundException(query.companyId()));
    }
}
