package com.company.companyservice.application.query;

import com.company.companyservice.domain.exception.CompanyNotFoundException;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.model.OfficerSummary;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.infrastructure.CompanyQueryRepository;
import com.company.companyservice.domain.port.infrastructure.OfficerQueryPort;
import com.company.companyservice.domain.port.usecases.GetCompanyUseCase;

import java.util.ArrayList;
import java.util.List;

public class GetCompanyHandler implements GetCompanyUseCase {

    private final CompanyQueryRepository queryRepo;
    private final OfficerQueryPort officerQueryPort;

    public GetCompanyHandler(CompanyQueryRepository queryRepo, OfficerQueryPort officerQueryPort) {
        this.queryRepo = queryRepo;
        this.officerQueryPort = officerQueryPort;
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
            return new Result(restricted, List.of());
        }

        OfficerQueryPort.OfficerQueryResult officerResult =
                officerQueryPort.findOfficersByCompanyId(query.companyId());

        List<OfficerSummary> officers = officerResult.officers();
        List<String> warnings = new ArrayList<>();

        if (officerResult.fallback()) {
            warnings.add("Officer service temporarily unavailable");
        }

        CompanyFullView withOfficers = new CompanyFullView(
                full.id(), full.name(), full.registrationNumber(),
                full.address(), full.ownerId(), full.ownerDisplayName(),
                full.status(), full.createdAt(), full.updatedAt(),
                officers
        );

        return new Result(withOfficers, List.copyOf(warnings));
    }
}
