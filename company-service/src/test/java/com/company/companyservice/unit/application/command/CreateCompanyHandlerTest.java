package com.company.companyservice.unit.application.command;

import com.company.companyservice.application.command.CreateCompanyHandler;
import com.company.companyservice.domain.event.CompanyCreatedEvent;
import com.company.companyservice.domain.exception.CompanyAccessDeniedException;
import com.company.companyservice.domain.exception.DuplicateRegistrationNumberException;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.usecases.CreateCompanyUseCase;
import com.company.companyservice.stubs.InMemoryCompanyCommandRepository;
import com.company.companyservice.stubs.InMemoryCompanyEventPublisher;
import com.company.companyservice.stubs.InMemoryCompanyQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateCompanyHandlerTest {

    private InMemoryCompanyCommandRepository commandRepo;
    private InMemoryCompanyQueryRepository queryRepo;
    private InMemoryCompanyEventPublisher publisher;
    private CreateCompanyHandler handler;

    @BeforeEach
    void setUp() {
        commandRepo = new InMemoryCompanyCommandRepository();
        queryRepo = new InMemoryCompanyQueryRepository();
        publisher = new InMemoryCompanyEventPublisher();
        handler = new CreateCompanyHandler(commandRepo, queryRepo, publisher);
    }

    @Test
    void userCreatesCompany() {
        UUID callerId = UUID.randomUUID();
        CreateCompanyUseCase.Command command = new CreateCompanyUseCase.Command(
                callerId, Role.USER, "Alice Smith",
                "Acme Corp", "REG-001",
                "1 Main St", "Paris", "75001", "France"
        );

        CompanyFullView result = handler.create(command);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Acme Corp");
        assertThat(result.registrationNumber()).isEqualTo("REG-001");
        assertThat(result.ownerId()).isEqualTo(callerId);
        assertThat(result.ownerDisplayName()).isEqualTo("Alice Smith");
        assertThat(result.officers()).isEmpty();
        assertThat(commandRepo.size()).isEqualTo(1);
        assertThat(publisher.publishedEvents()).hasSize(1);
        assertThat(publisher.lastEvent()).isInstanceOf(CompanyCreatedEvent.class);
    }

    @Test
    void adminCreatesCompany() {
        UUID callerId = UUID.randomUUID();
        CreateCompanyUseCase.Command command = new CreateCompanyUseCase.Command(
                callerId, Role.ADMIN, "Admin User",
                "Admin Corp", "REG-002",
                "2 Admin Ave", "Lyon", "69001", "France"
        );

        CompanyFullView result = handler.create(command);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Admin Corp");
        assertThat(commandRepo.size()).isEqualTo(1);
        assertThat(publisher.publishedEvents()).hasSize(1);
        assertThat(publisher.lastEvent()).isInstanceOf(CompanyCreatedEvent.class);
    }

    @Test
    void managerCannotCreate() {
        UUID callerId = UUID.randomUUID();
        CreateCompanyUseCase.Command command = new CreateCompanyUseCase.Command(
                callerId, Role.MANAGER, "Bob Manager",
                "Manager Corp", "REG-003",
                "3 Manager Blvd", "Marseille", "13001", "France"
        );

        assertThatThrownBy(() -> handler.create(command))
                .isInstanceOf(CompanyAccessDeniedException.class)
                .hasMessageContaining("MANAGER");

        assertThat(commandRepo.size()).isEqualTo(0);
        assertThat(publisher.publishedEvents()).isEmpty();
    }

    @Test
    void duplicateRegistrationNumberRejected() {
        UUID callerId = UUID.randomUUID();
        CreateCompanyUseCase.Command first = new CreateCompanyUseCase.Command(
                callerId, Role.USER, "Alice Smith",
                "Acme Corp", "REG-DUP",
                "1 Main St", "Paris", "75001", "France"
        );
        handler.create(first);

        UUID secondCallerId = UUID.randomUUID();
        CreateCompanyUseCase.Command second = new CreateCompanyUseCase.Command(
                secondCallerId, Role.USER, "Bob Jones",
                "Other Corp", "REG-DUP",
                "2 Other St", "Lyon", "69001", "France"
        );

        assertThatThrownBy(() -> handler.create(second))
                .isInstanceOf(DuplicateRegistrationNumberException.class)
                .hasMessageContaining("REG-DUP");

        assertThat(commandRepo.size()).isEqualTo(1);
    }
}
