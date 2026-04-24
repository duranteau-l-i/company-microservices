package com.company.officerservice.application.command;

import com.company.officerservice.domain.exception.OfficerAccessDeniedException;
import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.infrastructure.OfficerCommandRepository;
import com.company.officerservice.domain.port.infrastructure.OfficerEventPublisher;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import com.company.officerservice.domain.port.usecases.CreateOfficerUseCase;

import java.util.Objects;

public class CreateOfficerHandler implements CreateOfficerUseCase {

    private final OfficerCommandRepository commandRepo;
    private final OfficerQueryRepository queryRepo;
    private final OfficerEventPublisher publisher;

    public CreateOfficerHandler(OfficerCommandRepository commandRepo,
                                OfficerQueryRepository queryRepo,
                                OfficerEventPublisher publisher) {
        this.commandRepo = Objects.requireNonNull(commandRepo, "commandRepo");
        this.queryRepo = Objects.requireNonNull(queryRepo, "queryRepo");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    @Override
    public OfficerFullView create(Command command) {
        if (command.callerRole() == Role.USER && !command.callerId().equals(command.companyOwnerId())) {
            throw new OfficerAccessDeniedException("USER can only create officers for their own company");
        }

        Address address = new Address(command.street(), command.city(), command.postalCode(), command.country());

        Officer.Created created = Officer.create(
                command.firstName(), command.lastName(), command.dateOfBirth(),
                command.nationality(), address, command.email(), command.phone()
        );
        Officer officer = created.officer();

        CompanyLink link = CompanyLink.create(command.companyId(), command.title(), command.appointmentDate());
        Officer.Linked linked = officer.linkToCompany(link);

        commandRepo.save(officer);
        publisher.publish(created.event());
        publisher.publish(linked.event());

        OfficerFullView fullView = toFullView(officer);
        queryRepo.save(fullView);
        return fullView;
    }

    static OfficerFullView toFullView(Officer officer) {
        return new OfficerFullView(
                officer.id(), officer.firstName(), officer.lastName(), officer.dateOfBirth(),
                officer.nationality(), officer.address(), officer.email(), officer.phone(),
                officer.companyLinks(), officer.createdAt(), officer.updatedAt()
        );
    }
}
