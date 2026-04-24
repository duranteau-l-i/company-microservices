package com.company.officerservice.application.command;

import com.company.officerservice.domain.event.OfficerDeletedEvent;
import com.company.officerservice.domain.exception.OfficerAccessDeniedException;
import com.company.officerservice.domain.exception.OfficerNotFoundException;
import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.infrastructure.OfficerCommandRepository;
import com.company.officerservice.domain.port.infrastructure.OfficerEventPublisher;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import com.company.officerservice.domain.port.usecases.DeleteOfficerUseCase;

import java.util.Objects;

public class DeleteOfficerHandler implements DeleteOfficerUseCase {

    private final OfficerCommandRepository commandRepo;
    private final OfficerQueryRepository queryRepo;
    private final OfficerEventPublisher publisher;

    public DeleteOfficerHandler(OfficerCommandRepository commandRepo,
                                OfficerQueryRepository queryRepo,
                                OfficerEventPublisher publisher) {
        this.commandRepo = Objects.requireNonNull(commandRepo, "commandRepo");
        this.queryRepo = Objects.requireNonNull(queryRepo, "queryRepo");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    @Override
    public void delete(Command command) {
        if (command.callerRole() != Role.ADMIN) {
            throw new OfficerAccessDeniedException("Only ADMIN can delete officers");
        }

        Officer officer = commandRepo.findById(command.officerId())
                .orElseThrow(() -> new OfficerNotFoundException(command.officerId().value().toString()));

        OfficerDeletedEvent event = OfficerDeletedEvent.of(
                officer.id().value(), officer.firstName(), officer.lastName()
        );

        commandRepo.delete(command.officerId());
        queryRepo.deleteById(command.officerId());
        publisher.publish(event);
    }
}
