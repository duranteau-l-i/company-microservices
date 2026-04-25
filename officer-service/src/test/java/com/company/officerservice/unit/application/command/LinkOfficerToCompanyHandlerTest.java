package com.company.officerservice.unit.application.command;

import com.company.officerservice.application.command.LinkOfficerToCompanyHandler;
import com.company.officerservice.domain.event.OfficerLinkedToCompanyEvent;
import com.company.officerservice.domain.exception.CompanyNotFoundException;
import com.company.officerservice.domain.exception.DuplicateLinkException;
import com.company.officerservice.domain.exception.OfficerAccessDeniedException;
import com.company.officerservice.domain.exception.ServiceUnavailableException;
import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.usecases.LinkOfficerToCompanyUseCase;
import com.company.officerservice.stubs.InMemoryCompanyValidationPort;
import com.company.officerservice.stubs.InMemoryOfficerCommandRepository;
import com.company.officerservice.stubs.InMemoryOfficerEventPublisher;
import com.company.officerservice.stubs.InMemoryOfficerQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinkOfficerToCompanyHandlerTest {

    private InMemoryOfficerCommandRepository commandRepo;
    private InMemoryOfficerQueryRepository queryRepo;
    private InMemoryOfficerEventPublisher publisher;
    private InMemoryCompanyValidationPort companyValidationPort;
    private LinkOfficerToCompanyHandler handler;

    private Officer seedOfficer;
    private final UUID ownerId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        commandRepo = new InMemoryOfficerCommandRepository();
        queryRepo = new InMemoryOfficerQueryRepository();
        publisher = new InMemoryOfficerEventPublisher();
        companyValidationPort = new InMemoryCompanyValidationPort();
        companyValidationPort.addCompany(companyId);
        handler = new LinkOfficerToCompanyHandler(commandRepo, queryRepo, publisher, companyValidationPort);

        Officer.Created created = Officer.create(
                "Alice", "Smith", LocalDate.of(1990, 1, 15),
                "French", new Address("1 Rue", "Paris", "75001", "France"),
                "alice@example.com", null
        );
        seedOfficer = created.officer();
        commandRepo.save(seedOfficer);
        queryRepo.save(toFullView(seedOfficer));
    }

    @Test
    void companyOwnerLinksOfficer() {
        OfficerFullView result = handler.link(linkCommand(ownerId, Role.USER, ownerId));

        assertThat(result.companyLinks()).hasSize(1);
        assertThat(result.companyLinks().get(0).companyId()).isEqualTo(companyId);
        assertThat(publisher.publishedEvents()).hasSize(1);
        assertThat(publisher.lastEvent()).isInstanceOf(OfficerLinkedToCompanyEvent.class);
    }

    @Test
    void managerLinksOfficerToAnyCompany() {
        OfficerFullView result = handler.link(linkCommand(UUID.randomUUID(), Role.MANAGER, UUID.randomUUID()));

        assertThat(result.companyLinks()).hasSize(1);
    }

    @Test
    void adminLinksOfficer() {
        OfficerFullView result = handler.link(linkCommand(UUID.randomUUID(), Role.ADMIN, UUID.randomUUID()));

        assertThat(result.companyLinks()).hasSize(1);
    }

    @Test
    void nonOwnerUserCannotLink() {
        UUID notOwner = UUID.randomUUID();

        assertThatThrownBy(() -> handler.link(linkCommand(notOwner, Role.USER, ownerId)))
                .isInstanceOf(OfficerAccessDeniedException.class);

        assertThat(publisher.publishedEvents()).isEmpty();
    }

    @Test
    void duplicateLinkRejected() {
        handler.link(linkCommand(ownerId, Role.USER, ownerId));
        publisher.clear();

        assertThatThrownBy(() -> handler.link(linkCommand(ownerId, Role.USER, ownerId)))
                .isInstanceOf(DuplicateLinkException.class);

        assertThat(publisher.publishedEvents()).isEmpty();
    }

    @Test
    void linkRejected_whenCompanyDoesNotExist() {
        companyValidationPort.clear();

        assertThatThrownBy(() -> handler.link(linkCommand(ownerId, Role.USER, ownerId)))
                .isInstanceOf(CompanyNotFoundException.class);

        assertThat(publisher.publishedEvents()).isEmpty();
    }

    @Test
    void linkRejected_whenCompanyServiceUnavailable() {
        companyValidationPort.setSimulateUnavailable(true);

        assertThatThrownBy(() -> handler.link(linkCommand(ownerId, Role.USER, ownerId)))
                .isInstanceOf(ServiceUnavailableException.class);

        assertThat(publisher.publishedEvents()).isEmpty();
    }

    private LinkOfficerToCompanyUseCase.Command linkCommand(UUID callerId, Role role, UUID companyOwnerId) {
        return new LinkOfficerToCompanyUseCase.Command(
                callerId, role, companyOwnerId,
                seedOfficer.id(), companyId,
                "Director", LocalDate.of(2024, 1, 1)
        );
    }

    private static OfficerFullView toFullView(Officer officer) {
        return new OfficerFullView(
                officer.id(), officer.firstName(), officer.lastName(), officer.dateOfBirth(),
                officer.nationality(), officer.address(), officer.email(), officer.phone(),
                officer.companyLinks(), officer.createdAt(), officer.updatedAt()
        );
    }
}
