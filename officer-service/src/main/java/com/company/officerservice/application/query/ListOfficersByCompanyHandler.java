package com.company.officerservice.application.query;

import com.company.officerservice.domain.exception.OfficerAccessDeniedException;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerRestrictedView;
import com.company.officerservice.domain.model.OfficerView;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.infrastructure.CompanyValidationPort;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import com.company.officerservice.domain.port.usecases.ListOfficersByCompanyUseCase;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ListOfficersByCompanyHandler implements ListOfficersByCompanyUseCase {

    private final OfficerQueryRepository queryRepo;
    private final CompanyValidationPort companyValidationPort;

    public ListOfficersByCompanyHandler(OfficerQueryRepository queryRepo,
                                        CompanyValidationPort companyValidationPort) {
        this.queryRepo = Objects.requireNonNull(queryRepo, "queryRepo");
        this.companyValidationPort = Objects.requireNonNull(companyValidationPort, "companyValidationPort");
    }

    @Override
    public List<OfficerView> list(Command command) {
        if (!command.callerRole().isAtLeast(Role.MANAGER)) {
            UUID ownerId = companyValidationPort.findOwnerId(command.companyId())
                    .orElseThrow(() -> new OfficerAccessDeniedException("USER can only list officers for their own company"));
            if (!ownerId.equals(command.callerId())) {
                throw new OfficerAccessDeniedException("USER can only list officers for their own company");
            }
        }

        List<OfficerFullView> full = queryRepo.findByCompanyId(command.companyId());

        if (command.callerRole().isAtLeast(Role.MANAGER)) {
            return full.stream().map(v -> (OfficerView) v).toList();
        }
        return full.stream()
                .map(v -> (OfficerView) new OfficerRestrictedView(v.id(), v.firstName(), v.lastName(), v.companyLinks()))
                .toList();
    }
}
