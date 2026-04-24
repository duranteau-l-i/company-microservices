package com.company.companyservice.unit.application.command;

import com.company.companyservice.application.command.UpdateCompanyHandler;
import com.company.companyservice.domain.event.CompanyUpdatedEvent;
import com.company.companyservice.domain.exception.CompanyAccessDeniedException;
import com.company.companyservice.domain.exception.CompanyNotFoundException;
import com.company.companyservice.domain.exception.DuplicateRegistrationNumberException;
import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.Company;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyStatus;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.usecases.UpdateCompanyUseCase;
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

class UpdateCompanyHandlerTest {

    private InMemoryCompanyCommandRepository commandRepo;
    private InMemoryCompanyQueryRepository queryRepo;
    private InMemoryCompanyEventPublisher publisher;
    private UpdateCompanyHandler handler;

    private UUID ownerId;
    private CompanyId companyId;

    @BeforeEach
    void setUp() {
        commandRepo = new InMemoryCompanyCommandRepository();
        queryRepo = new InMemoryCompanyQueryRepository();
        publisher = new InMemoryCompanyEventPublisher();
        handler = new UpdateCompanyHandler(commandRepo, queryRepo, publisher);

        ownerId = UUID.randomUUID();
        seedCompany(ownerId, "REG-SEED");
    }

    private void seedCompany(UUID ownerUuid, String regNum) {
        Instant now = Instant.now();
        Company company = new Company(
                companyId = CompanyId.generate(),
                "Original Corp", regNum,
                new Address("1 Old St", "Paris", "75001", "France"),
                ownerUuid, CompanyStatus.ACTIVE, now, now
        );
        commandRepo.save(company);

        CompanyFullView view = new CompanyFullView(
                companyId, "Original Corp", regNum,
                new Address("1 Old St", "Paris", "75001", "France"),
                ownerUuid, "Owner Name", CompanyStatus.ACTIVE,
                now, now, List.of()
        );
        queryRepo.save(view);
    }

    @Test
    void ownerUpdatesOwnCompany() {
        UpdateCompanyUseCase.Command command = new UpdateCompanyUseCase.Command(
                ownerId, Role.USER, companyId,
                "Updated Corp", "REG-SEED",
                "2 New St", "Lyon", "69001", "France"
        );

        CompanyFullView result = handler.update(command);

        assertThat(result.name()).isEqualTo("Updated Corp");
        assertThat(result.address().city()).isEqualTo("Lyon");
        assertThat(result.ownerDisplayName()).isEqualTo("Owner Name");
        assertThat(result.officers()).isEmpty();
        assertThat(publisher.publishedEvents()).hasSize(1);
        assertThat(publisher.lastEvent()).isInstanceOf(CompanyUpdatedEvent.class);
    }

    @Test
    void nonOwnerUserCannotUpdate() {
        UUID otherId = UUID.randomUUID();
        UpdateCompanyUseCase.Command command = new UpdateCompanyUseCase.Command(
                otherId, Role.USER, companyId,
                "Hijacked Corp", "REG-SEED",
                "2 New St", "Lyon", "69001", "France"
        );

        assertThatThrownBy(() -> handler.update(command))
                .isInstanceOf(CompanyAccessDeniedException.class);

        assertThat(publisher.publishedEvents()).isEmpty();
    }

    @Test
    void managerUpdatesAnyCompany() {
        UUID managerId = UUID.randomUUID();
        UpdateCompanyUseCase.Command command = new UpdateCompanyUseCase.Command(
                managerId, Role.MANAGER, companyId,
                "Manager Updated", "REG-SEED",
                "3 Mgr Blvd", "Marseille", "13001", "France"
        );

        CompanyFullView result = handler.update(command);

        assertThat(result.name()).isEqualTo("Manager Updated");
        assertThat(publisher.publishedEvents()).hasSize(1);
        assertThat(publisher.lastEvent()).isInstanceOf(CompanyUpdatedEvent.class);
    }

    @Test
    void adminUpdatesAnyCompany() {
        UUID adminId = UUID.randomUUID();
        UpdateCompanyUseCase.Command command = new UpdateCompanyUseCase.Command(
                adminId, Role.ADMIN, companyId,
                "Admin Updated", "REG-SEED",
                "4 Admin Rd", "Nantes", "44000", "France"
        );

        CompanyFullView result = handler.update(command);

        assertThat(result.name()).isEqualTo("Admin Updated");
        assertThat(publisher.publishedEvents()).hasSize(1);
        assertThat(publisher.lastEvent()).isInstanceOf(CompanyUpdatedEvent.class);
    }

    @Test
    void updateNotFound() {
        CompanyId unknownId = CompanyId.generate();
        UpdateCompanyUseCase.Command command = new UpdateCompanyUseCase.Command(
                ownerId, Role.USER, unknownId,
                "Ghost Corp", "REG-GHOST",
                "1 Ghost St", "Paris", "75001", "France"
        );

        assertThatThrownBy(() -> handler.update(command))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    void duplicateRegistrationOnUpdate() {
        // Seed a second company with a different reg number
        UUID otherOwner = UUID.randomUUID();
        Instant now = Instant.now();
        CompanyId otherId = CompanyId.generate();
        Company other = new Company(
                otherId, "Other Corp", "REG-OTHER",
                new Address("5 Other St", "Paris", "75001", "France"),
                otherOwner, CompanyStatus.ACTIVE, now, now
        );
        commandRepo.save(other);
        queryRepo.save(new CompanyFullView(
                otherId, "Other Corp", "REG-OTHER",
                new Address("5 Other St", "Paris", "75001", "France"),
                otherOwner, "Other Owner", CompanyStatus.ACTIVE, now, now, List.of()
        ));

        // Try to update the first company with the second company's reg number
        UpdateCompanyUseCase.Command command = new UpdateCompanyUseCase.Command(
                ownerId, Role.USER, companyId,
                "Updated Corp", "REG-OTHER",
                "2 New St", "Lyon", "69001", "France"
        );

        assertThatThrownBy(() -> handler.update(command))
                .isInstanceOf(DuplicateRegistrationNumberException.class)
                .hasMessageContaining("REG-OTHER");
    }
}
