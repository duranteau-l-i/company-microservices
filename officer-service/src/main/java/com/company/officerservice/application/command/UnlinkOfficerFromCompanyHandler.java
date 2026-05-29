package com.company.officerservice.application.command;

import com.company.officerservice.domain.exception.CompanyNotFoundException;
import com.company.officerservice.domain.exception.OfficerAccessDeniedException;
import com.company.officerservice.domain.exception.OfficerNotFoundException;
import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.infrastructure.CompanyValidationPort;
import com.company.officerservice.domain.port.infrastructure.OfficerCommandRepository;
import com.company.officerservice.domain.port.infrastructure.OfficerEventPublisher;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import com.company.officerservice.domain.port.usecases.UnlinkOfficerFromCompanyUseCase;

import java.util.Objects;
import java.util.UUID;

public class UnlinkOfficerFromCompanyHandler implements UnlinkOfficerFromCompanyUseCase {

    private final OfficerCommandRepository commandRepo;
    private final OfficerQueryRepository queryRepo;
    private final OfficerEventPublisher publisher;
    private final CompanyValidationPort companyValidationPort;

    public UnlinkOfficerFromCompanyHandler(OfficerCommandRepository commandRepo,
                                           OfficerQueryRepository queryRepo,
                                           OfficerEventPublisher publisher,
                                           CompanyValidationPort companyValidationPort) {
        this.commandRepo = Objects.requireNonNull(commandRepo, "commandRepo");
        this.queryRepo = Objects.requireNonNull(queryRepo, "queryRepo");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.companyValidationPort = Objects.requireNonNull(companyValidationPort, "companyValidationPort");
    }

    @Override
    public OfficerFullView unlink(Command command) {
        if (command.callerRole() == Role.USER) {
            UUID realOwnerId = companyValidationPort.findOwnerId(command.companyId())
                    .orElseThrow(() -> new CompanyNotFoundException(command.companyId()));
            if (!realOwnerId.equals(command.callerId())) {
                throw new OfficerAccessDeniedException("USER can only unlink officers from their own company");
            }
        }

        Officer officer = commandRepo.findById(command.officerId())
                .orElseThrow(() -> new OfficerNotFoundException(command.officerId().value().toString()));

        Officer.Unlinked unlinked = officer.unlinkFromCompany(command.companyId());

        commandRepo.save(unlinked.officer());
        publisher.publish(unlinked.event());

        OfficerFullView fullView = toFullView(unlinked.officer());
        queryRepo.save(fullView);
        return fullView;
    }

    private static OfficerFullView toFullView(Officer officer) {
        return new OfficerFullView(
                officer.id(), officer.firstName(), officer.lastName(), officer.dateOfBirth(),
                officer.nationality(), officer.address(), officer.email(), officer.phone(),
                officer.companyLinks(), officer.createdAt(), officer.updatedAt()
        );
    }
}
