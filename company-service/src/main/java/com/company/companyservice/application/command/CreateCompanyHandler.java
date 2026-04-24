package com.company.companyservice.application.command;

import com.company.companyservice.domain.exception.CompanyAccessDeniedException;
import com.company.companyservice.domain.exception.DuplicateRegistrationNumberException;
import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.Company;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.infrastructure.CompanyCommandRepository;
import com.company.companyservice.domain.port.infrastructure.CompanyEventPublisher;
import com.company.companyservice.domain.port.infrastructure.CompanyQueryRepository;
import com.company.companyservice.domain.port.usecases.CreateCompanyUseCase;

import java.util.List;
import java.util.Objects;

public class CreateCompanyHandler implements CreateCompanyUseCase {

    private final CompanyCommandRepository commandRepo;
    private final CompanyQueryRepository queryRepo;
    private final CompanyEventPublisher publisher;

    public CreateCompanyHandler(CompanyCommandRepository commandRepo,
                                CompanyQueryRepository queryRepo,
                                CompanyEventPublisher publisher) {
        this.commandRepo = Objects.requireNonNull(commandRepo, "commandRepo");
        this.queryRepo = Objects.requireNonNull(queryRepo, "queryRepo");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    @Override
    public CompanyFullView create(Command command) {
        if (command.callerRole() == Role.MANAGER) {
            throw new CompanyAccessDeniedException("MANAGER cannot create companies");
        }

        if (commandRepo.existsByRegistrationNumber(command.registrationNumber())) {
            throw new DuplicateRegistrationNumberException(command.registrationNumber());
        }

        Address address = new Address(
                command.street(), command.city(), command.postalCode(), command.country()
        );

        Company.Created result = Company.create(
                command.name(), command.registrationNumber(), address, command.callerId()
        );

        commandRepo.save(result.company());
        publisher.publish(result.event());

        Company company = result.company();
        CompanyFullView fullView = new CompanyFullView(
                company.id(),
                company.name(),
                company.registrationNumber(),
                company.address(),
                company.ownerId(),
                command.ownerDisplayName(),
                company.status(),
                company.createdAt(),
                company.updatedAt(),
                List.of()
        );

        queryRepo.save(fullView);

        return fullView;
    }
}
