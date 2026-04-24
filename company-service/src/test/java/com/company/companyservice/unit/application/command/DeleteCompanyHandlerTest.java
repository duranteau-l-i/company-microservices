package com.company.companyservice.unit.application.command;

import com.company.companyservice.application.command.DeleteCompanyHandler;
import com.company.companyservice.domain.event.CompanyDeletedEvent;
import com.company.companyservice.domain.exception.CompanyAccessDeniedException;
import com.company.companyservice.domain.exception.CompanyNotFoundException;
import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.Company;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyStatus;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.usecases.DeleteCompanyUseCase;
import com.company.companyservice.unit.application.stubs.InMemoryCompanyCommandRepository;
import com.company.companyservice.unit.application.stubs.InMemoryCompanyEventPublisher;
import com.company.companyservice.unit.application.stubs.InMemoryCompanyQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeleteCompanyHandlerTest {

    private InMemoryCompanyCommandRepository commandRepo;
    private InMemoryCompanyQueryRepository queryRepo;
    private InMemoryCompanyEventPublisher publisher;
    private DeleteCompanyHandler handler;

    private UUID ownerId;
    private CompanyId companyId;

    @BeforeEach
    void setUp() {
        commandRepo = new InMemoryCompanyCommandRepository();
        queryRepo = new InMemoryCompanyQueryRepository();
        publisher = new InMemoryCompanyEventPublisher();
        handler = new DeleteCompanyHandler(commandRepo, queryRepo, publisher);

        ownerId = UUID.randomUUID();
        companyId = seedCompany(ownerId);
    }

    private CompanyId seedCompany(UUID ownerUuid) {
        Instant now = Instant.now();
        CompanyId id = CompanyId.generate();
        Company company = new Company(
                id, "Seeded Corp", "REG-SEED",
                new Address("1 Old St", "Paris", "75001", "France"),
                ownerUuid, CompanyStatus.ACTIVE, now, now
        );
        commandRepo.save(company);

        CompanyFullView view = new CompanyFullView(
                id, "Seeded Corp", "REG-SEED",
                new Address("1 Old St", "Paris", "75001", "France"),
                ownerUuid, "Owner Name", CompanyStatus.ACTIVE, now, now, List.of()
        );
        queryRepo.save(view);
        return id;
    }

    @Test
    void ownerDeletesOwnCompany() {
        DeleteCompanyUseCase.Command command = new DeleteCompanyUseCase.Command(ownerId, Role.USER, companyId);

        handler.delete(command);

        assertThat(commandRepo.size()).isEqualTo(0);
        assertThat(queryRepo.size()).isEqualTo(0);
        assertThat(publisher.publishedEvents()).hasSize(1);
        assertThat(publisher.lastEvent()).isInstanceOf(CompanyDeletedEvent.class);
    }

    @Test
    void nonOwnerUserCannotDelete() {
        UUID otherId = UUID.randomUUID();
        DeleteCompanyUseCase.Command command = new DeleteCompanyUseCase.Command(otherId, Role.USER, companyId);

        assertThatThrownBy(() -> handler.delete(command))
                .isInstanceOf(CompanyAccessDeniedException.class);

        assertThat(commandRepo.size()).isEqualTo(1);
        assertThat(queryRepo.size()).isEqualTo(1);
        assertThat(publisher.publishedEvents()).isEmpty();
    }

    @Test
    void managerCannotDelete() {
        UUID managerId = UUID.randomUUID();
        DeleteCompanyUseCase.Command command = new DeleteCompanyUseCase.Command(managerId, Role.MANAGER, companyId);

        assertThatThrownBy(() -> handler.delete(command))
                .isInstanceOf(CompanyAccessDeniedException.class)
                .hasMessageContaining("MANAGER");

        assertThat(commandRepo.size()).isEqualTo(1);
        assertThat(queryRepo.size()).isEqualTo(1);
        assertThat(publisher.publishedEvents()).isEmpty();
    }

    @Test
    void adminDeletesAnyCompany() {
        UUID adminId = UUID.randomUUID();
        DeleteCompanyUseCase.Command command = new DeleteCompanyUseCase.Command(adminId, Role.ADMIN, companyId);

        handler.delete(command);

        assertThat(commandRepo.size()).isEqualTo(0);
        assertThat(queryRepo.size()).isEqualTo(0);
        assertThat(publisher.publishedEvents()).hasSize(1);
        assertThat(publisher.lastEvent()).isInstanceOf(CompanyDeletedEvent.class);
    }

    @Test
    void deleteNotFound() {
        CompanyId unknownId = CompanyId.generate();
        DeleteCompanyUseCase.Command command = new DeleteCompanyUseCase.Command(ownerId, Role.USER, unknownId);

        assertThatThrownBy(() -> handler.delete(command))
                .isInstanceOf(CompanyNotFoundException.class);
    }
}
