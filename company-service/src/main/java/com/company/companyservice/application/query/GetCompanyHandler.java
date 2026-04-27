package com.company.companyservice.application.query;

import com.company.companyservice.domain.exception.CompanyNotFoundException;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.infrastructure.CompanyQueryRepository;
import com.company.companyservice.domain.port.usecases.GetCompanyUseCase;

public class GetCompanyHandler implements GetCompanyUseCase {

    private final CompanyQueryRepository queryRepo;

    public GetCompanyHandler(CompanyQueryRepository queryRepo) {
        this.queryRepo = queryRepo;
    }

    @Override
    public Result get(Query query) {
        CompanyFullView full = queryRepo.findFullById(query.companyId())
                .orElseThrow(() -> new CompanyNotFoundException(query.companyId()));

        boolean isOwner = full.ownerId().equals(query.callerId());
        boolean canSeeFull = query.callerRole().isAtLeast(Role.MANAGER) || isOwner;

        if (!canSeeFull) {
            CompanyRestrictedView restricted = new CompanyRestrictedView(
                    full.id(), full.name(), full.registrationNumber(),
                    full.ownerId(), full.ownerDisplayName(), full.status()
            );
            return new Result(restricted);
        }

        return new Result(full);
    }
}
