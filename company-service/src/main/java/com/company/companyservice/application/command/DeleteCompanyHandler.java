package com.company.companyservice.application.command;

import com.company.companyservice.domain.event.CompanyDeletedEvent;
import com.company.companyservice.domain.exception.CompanyAccessDeniedException;
import com.company.companyservice.domain.exception.CompanyNotFoundException;
import com.company.companyservice.domain.model.Company;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.infrastructure.CompanyCommandRepository;
import com.company.companyservice.domain.port.infrastructure.CompanyEventPublisher;
import com.company.companyservice.domain.port.infrastructure.CompanyQueryRepository;
import com.company.companyservice.domain.port.usecases.DeleteCompanyUseCase;

import java.time.Instant;
import java.util.Objects;

public class DeleteCompanyHandler implements DeleteCompanyUseCase {

    private final CompanyCommandRepository commandRepo;
    private final CompanyQueryRepository queryRepo;
    private final CompanyEventPublisher publisher;

    public DeleteCompanyHandler(CompanyCommandRepository commandRepo,
                                CompanyQueryRepository queryRepo,
                                CompanyEventPublisher publisher) {
        this.commandRepo = Objects.requireNonNull(commandRepo, "commandRepo");
        this.queryRepo = Objects.requireNonNull(queryRepo, "queryRepo");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    @Override
    public void delete(Command command) {
        Company company = commandRepo.findById(command.companyId())
                .orElseThrow(() -> new CompanyNotFoundException(command.companyId()));

        boolean isOwner = company.ownerId().equals(command.callerId());

        if (command.callerRole() == Role.MANAGER) {
            throw new CompanyAccessDeniedException("MANAGER cannot delete companies");
        }

        if (!command.callerRole().isAtLeast(Role.ADMIN) && !isOwner) {
            throw new CompanyAccessDeniedException("Access denied: not the owner and insufficient role");
        }

        CompanyDeletedEvent event = CompanyDeletedEvent.of(company.id(), company.ownerId(), Instant.now());

        commandRepo.delete(command.companyId());
        queryRepo.deleteById(command.companyId());
        publisher.publish(event);
    }
}
