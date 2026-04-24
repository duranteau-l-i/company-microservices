package com.company.officerservice.application.query;

import com.company.officerservice.domain.exception.OfficerNotFoundException;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerRestrictedView;
import com.company.officerservice.domain.model.OfficerView;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import com.company.officerservice.domain.port.usecases.GetOfficerUseCase;

import java.util.Objects;

public class GetOfficerHandler implements GetOfficerUseCase {

    private final OfficerQueryRepository queryRepo;

    public GetOfficerHandler(OfficerQueryRepository queryRepo) {
        this.queryRepo = Objects.requireNonNull(queryRepo, "queryRepo");
    }

    @Override
    public OfficerView get(Command command) {
        OfficerFullView full = queryRepo.findFullById(command.officerId())
                .orElseThrow(() -> new OfficerNotFoundException(command.officerId().value().toString()));

        if (command.callerRole().isAtLeast(Role.MANAGER)) {
            return full;
        }
        return new OfficerRestrictedView(full.id(), full.firstName(), full.lastName(), full.companyLinks());
    }
}
