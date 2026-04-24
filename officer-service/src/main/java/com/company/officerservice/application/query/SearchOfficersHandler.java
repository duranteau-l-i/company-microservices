package com.company.officerservice.application.query;

import com.company.officerservice.domain.model.OfficerRestrictedView;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import com.company.officerservice.domain.port.usecases.SearchOfficersUseCase;

import java.util.List;
import java.util.Objects;

public class SearchOfficersHandler implements SearchOfficersUseCase {

    private final OfficerQueryRepository queryRepo;

    public SearchOfficersHandler(OfficerQueryRepository queryRepo) {
        this.queryRepo = Objects.requireNonNull(queryRepo, "queryRepo");
    }

    @Override
    public List<OfficerRestrictedView> search(Command command) {
        return queryRepo.search(command.firstName(), command.lastName(), command.dateOfBirth());
    }
}
