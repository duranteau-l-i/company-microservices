package com.company.companyservice.application.command;

import com.company.companyservice.domain.exception.CompanyAccessDeniedException;
import com.company.companyservice.domain.exception.CompanyNotFoundException;
import com.company.companyservice.domain.exception.DuplicateRegistrationNumberException;
import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.Company;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.infrastructure.CompanyCommandRepository;
import com.company.companyservice.domain.port.infrastructure.CompanyEventPublisher;
import com.company.companyservice.domain.port.infrastructure.CompanyQueryRepository;
import com.company.companyservice.domain.port.usecases.UpdateCompanyUseCase;

import java.util.Objects;

public class UpdateCompanyHandler implements UpdateCompanyUseCase {

    private final CompanyCommandRepository commandRepo;
    private final CompanyQueryRepository queryRepo;
    private final CompanyEventPublisher publisher;

    public UpdateCompanyHandler(CompanyCommandRepository commandRepo,
                                CompanyQueryRepository queryRepo,
                                CompanyEventPublisher publisher) {
        this.commandRepo = Objects.requireNonNull(commandRepo, "commandRepo");
        this.queryRepo = Objects.requireNonNull(queryRepo, "queryRepo");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    @Override
    public CompanyFullView update(Command command) {
        Company company = commandRepo.findById(command.companyId())
                .orElseThrow(() -> new CompanyNotFoundException(command.companyId()));

        boolean isOwner = company.ownerId().equals(command.callerId());

        if (!command.callerRole().isAtLeast(Role.MANAGER) && !isOwner) {
            throw new CompanyAccessDeniedException("Access denied: not the owner and insufficient role");
        }

        if (!command.registrationNumber().equals(company.registrationNumber())
                && commandRepo.existsByRegistrationNumber(command.registrationNumber())) {
            throw new DuplicateRegistrationNumberException(command.registrationNumber());
        }

        Address address = new Address(
                command.street(), command.city(), command.postalCode(), command.country()
        );

        Company.Updated updated = company.update(command.name(), command.registrationNumber(), address);

        commandRepo.save(updated.company());
        publisher.publish(updated.event());

        CompanyFullView existingView = queryRepo.findFullById(command.companyId())
                .orElseThrow(() -> new CompanyNotFoundException(command.companyId()));

        Company updatedCompany = updated.company();
        CompanyFullView newFullView = new CompanyFullView(
                updatedCompany.id(),
                updatedCompany.name(),
                updatedCompany.registrationNumber(),
                updatedCompany.address(),
                updatedCompany.ownerId(),
                existingView.ownerDisplayName(),
                updatedCompany.status(),
                updatedCompany.createdAt(),
                updatedCompany.updatedAt(),
                existingView.officers()
        );

        queryRepo.save(newFullView);

        return newFullView;
    }
}
