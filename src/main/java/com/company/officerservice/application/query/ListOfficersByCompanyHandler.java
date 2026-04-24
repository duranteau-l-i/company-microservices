package com.company.officerservice.application.query;

import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import com.company.officerservice.domain.port.usecases.ListOfficersByCompanyUseCase;

import java.util.List;
import java.util.Objects;

public class ListOfficersByCompanyHandler implements ListOfficersByCompanyUseCase {

    private final OfficerQueryRepository queryRepo;

    public ListOfficersByCompanyHandler(OfficerQueryRepository queryRepo) {
        this.queryRepo = Objects.requireNonNull(queryRepo, "queryRepo");
    }

    @Override
    public List<OfficerFullView> list(Command command) {
        return queryRepo.findByCompanyId(command.companyId());
    }
}
