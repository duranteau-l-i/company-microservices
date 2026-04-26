package com.company.officerservice.application.command;

import com.company.officerservice.domain.exception.CompanyNotFoundException;
import com.company.officerservice.domain.exception.OfficerAccessDeniedException;
import com.company.officerservice.domain.exception.OfficerNotFoundException;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.infrastructure.CompanyValidationPort;
import com.company.officerservice.domain.port.infrastructure.OfficerCommandRepository;
import com.company.officerservice.domain.port.infrastructure.OfficerEventPublisher;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import com.company.officerservice.domain.port.usecases.LinkOfficerToCompanyUseCase;

import java.util.Objects;

public class LinkOfficerToCompanyHandler implements LinkOfficerToCompanyUseCase {

    private final OfficerCommandRepository commandRepo;
    private final OfficerQueryRepository queryRepo;
    private final OfficerEventPublisher publisher;
    private final CompanyValidationPort companyValidationPort;

    public LinkOfficerToCompanyHandler(OfficerCommandRepository commandRepo,
                                       OfficerQueryRepository queryRepo,
                                       OfficerEventPublisher publisher,
                                       CompanyValidationPort companyValidationPort) {
        this.commandRepo = Objects.requireNonNull(commandRepo, "commandRepo");
        this.queryRepo = Objects.requireNonNull(queryRepo, "queryRepo");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.companyValidationPort = Objects.requireNonNull(companyValidationPort, "companyValidationPort");
    }

    @Override
    public OfficerFullView link(Command command) {
        if (command.callerRole() == Role.USER && !command.callerId().equals(command.companyOwnerId())) {
            throw new OfficerAccessDeniedException("USER can only link officers to their own company");
        }

        if (!companyValidationPort.companyExists(command.companyId())) {
            throw new CompanyNotFoundException(command.companyId());
        }

        Officer officer = commandRepo.findById(command.officerId())
                .orElseThrow(() -> new OfficerNotFoundException(command.officerId().value().toString()));

        CompanyLink link = CompanyLink.create(command.companyId(), command.title(), command.appointmentDate());
        Officer.Linked linked = officer.linkToCompany(link);

        commandRepo.save(linked.officer());
        publisher.publish(linked.event());

        OfficerFullView fullView = toFullView(linked.officer());
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
