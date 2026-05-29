package com.company.officerservice.unit.application.command;

import com.company.officerservice.application.command.CreateOfficerHandler;
import com.company.officerservice.domain.event.OfficerCreatedEvent;
import com.company.officerservice.domain.event.OfficerLinkedToCompanyEvent;
import com.company.officerservice.domain.exception.CompanyNotFoundException;
import com.company.officerservice.domain.exception.OfficerAccessDeniedException;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.usecases.CreateOfficerUseCase;
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

class CreateOfficerHandlerTest {

    private InMemoryOfficerCommandRepository commandRepo;
    private InMemoryOfficerQueryRepository queryRepo;
    private InMemoryOfficerEventPublisher publisher;
    private InMemoryCompanyValidationPort companyValidationPort;
    private CreateOfficerHandler handler;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        commandRepo = new InMemoryOfficerCommandRepository();
        queryRepo = new InMemoryOfficerQueryRepository();
        publisher = new InMemoryOfficerEventPublisher();
        companyValidationPort = new InMemoryCompanyValidationPort();
        companyValidationPort.addCompany(companyId, ownerId);
        handler = new CreateOfficerHandler(commandRepo, queryRepo, publisher, companyValidationPort);
    }

    @Test
    void companyOwnerCreatesOfficer() {
        OfficerFullView result = handler.create(command(ownerId, Role.USER));

        assertThat(result).isNotNull();
        assertThat(result.firstName()).isEqualTo("Alice");
        assertThat(result.lastName()).isEqualTo("Smith");
        assertThat(result.companyLinks()).hasSize(1);
        assertThat(result.companyLinks().get(0).companyId()).isEqualTo(companyId);
        assertThat(commandRepo.size()).isEqualTo(1);
        assertThat(queryRepo.size()).isEqualTo(1);
    }

    @Test
    void managerCreatesOfficerForAnyCompany() {
        OfficerFullView result = handler.create(command(UUID.randomUUID(), Role.MANAGER));

        assertThat(result).isNotNull();
        assertThat(commandRepo.size()).isEqualTo(1);
    }

    @Test
    void adminCreatesOfficer() {
        OfficerFullView result = handler.create(command(UUID.randomUUID(), Role.ADMIN));

        assertThat(result).isNotNull();
        assertThat(commandRepo.size()).isEqualTo(1);
    }

    @Test
    void nonOwnerUserCannotCreate() {
        UUID notOwner = UUID.randomUUID();

        assertThatThrownBy(() -> handler.create(command(notOwner, Role.USER)))
                .isInstanceOf(OfficerAccessDeniedException.class);

        assertThat(commandRepo.size()).isEqualTo(0);
        assertThat(publisher.publishedEvents()).isEmpty();
    }

    @Test
    void userCannotCreateForUnknownCompany() {
        companyValidationPort.clear();

        assertThatThrownBy(() -> handler.create(command(ownerId, Role.USER)))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    void createPublishesTwoEvents() {
        handler.create(command(ownerId, Role.USER));

        assertThat(publisher.publishedEvents()).hasSize(2);
        assertThat(publisher.publishedEvents().get(0)).isInstanceOf(OfficerCreatedEvent.class);
        assertThat(publisher.publishedEvents().get(1)).isInstanceOf(OfficerLinkedToCompanyEvent.class);
    }

    private CreateOfficerUseCase.Command command(UUID callerId, Role role) {
        return new CreateOfficerUseCase.Command(
                callerId, role, companyId,
                "Alice", "Smith", LocalDate.of(1990, 1, 15),
                "French", "1 Rue de la Paix", "Paris", "75001", "France",
                "alice@example.com", "+33600000000",
                "Director", LocalDate.of(2024, 1, 1)
        );
    }
}
