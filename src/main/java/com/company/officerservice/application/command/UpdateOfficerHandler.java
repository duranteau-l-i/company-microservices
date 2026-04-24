package com.company.officerservice.application.command;

import com.company.officerservice.domain.exception.OfficerAccessDeniedException;
import com.company.officerservice.domain.exception.OfficerNotFoundException;
import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.infrastructure.OfficerCommandRepository;
import com.company.officerservice.domain.port.infrastructure.OfficerEventPublisher;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import com.company.officerservice.domain.port.usecases.UpdateOfficerUseCase;

import java.util.Objects;

public class UpdateOfficerHandler implements UpdateOfficerUseCase {

    private final OfficerCommandRepository commandRepo;
    private final OfficerQueryRepository queryRepo;
    private final OfficerEventPublisher publisher;

    public UpdateOfficerHandler(OfficerCommandRepository commandRepo,
                                OfficerQueryRepository queryRepo,
                                OfficerEventPublisher publisher) {
        this.commandRepo = Objects.requireNonNull(commandRepo, "commandRepo");
        this.queryRepo = Objects.requireNonNull(queryRepo, "queryRepo");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    @Override
    public OfficerFullView update(Command command) {
        if (!command.callerRole().isAtLeast(Role.MANAGER)) {
            throw new OfficerAccessDeniedException("Only MANAGER or ADMIN can update officers");
        }

        Officer officer = commandRepo.findById(command.officerId())
                .orElseThrow(() -> new OfficerNotFoundException(command.officerId().value().toString()));

        Address address = new Address(command.street(), command.city(), command.postalCode(), command.country());
        Officer.Updated updated = officer.update(
                command.firstName(), command.lastName(), command.nationality(),
                address, command.email(), command.phone()
        );

        commandRepo.save(updated.officer());
        publisher.publish(updated.event());

        OfficerFullView fullView = toFullView(updated.officer());
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
