package com.company.officerservice.application.query;

import com.company.officerservice.domain.exception.OfficerNotFoundException;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import com.company.officerservice.domain.port.usecases.ListCompaniesByOfficerUseCase;

import java.util.Objects;

public class ListCompaniesByOfficerHandler implements ListCompaniesByOfficerUseCase {

    private final OfficerQueryRepository queryRepo;

    public ListCompaniesByOfficerHandler(OfficerQueryRepository queryRepo) {
        this.queryRepo = Objects.requireNonNull(queryRepo, "queryRepo");
    }

    @Override
    public OfficerFullView list(Command command) {
        return queryRepo.findFullById(command.officerId())
                .orElseThrow(() -> new OfficerNotFoundException(command.officerId().value().toString()));
    }
}
