package com.company.officerservice.unit.application.command;

import com.company.officerservice.application.command.UnlinkOfficerFromCompanyHandler;
import com.company.officerservice.domain.event.OfficerUnlinkedFromCompanyEvent;
import com.company.officerservice.domain.exception.OfficerAccessDeniedException;
import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.usecases.UnlinkOfficerFromCompanyUseCase;
import com.company.officerservice.unit.application.stubs.InMemoryOfficerCommandRepository;
import com.company.officerservice.unit.application.stubs.InMemoryOfficerEventPublisher;
import com.company.officerservice.unit.application.stubs.InMemoryOfficerQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnlinkOfficerFromCompanyHandlerTest {

    private InMemoryOfficerCommandRepository commandRepo;
    private InMemoryOfficerQueryRepository queryRepo;
    private InMemoryOfficerEventPublisher publisher;
    private UnlinkOfficerFromCompanyHandler handler;

    private Officer seedOfficer;
    private final UUID ownerId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        commandRepo = new InMemoryOfficerCommandRepository();
        queryRepo = new InMemoryOfficerQueryRepository();
        publisher = new InMemoryOfficerEventPublisher();
        handler = new UnlinkOfficerFromCompanyHandler(commandRepo, queryRepo, publisher);

        Officer.Created created = Officer.create(
                "Alice", "Smith", LocalDate.of(1990, 1, 15),
                "French", new Address("1 Rue", "Paris", "75001", "France"),
                "alice@example.com", null
        );
        seedOfficer = created.officer();
        CompanyLink link = CompanyLink.create(companyId, "Director", LocalDate.of(2024, 1, 1));
        seedOfficer.linkToCompany(link);
        commandRepo.save(seedOfficer);
        queryRepo.save(toFullView(seedOfficer));
    }

    @Test
    void companyOwnerUnlinksOfficer() {
        OfficerFullView result = handler.unlink(unlinkCommand(ownerId, Role.USER, ownerId));

        assertThat(result.companyLinks()).allMatch(l -> !l.active());
        assertThat(publisher.publishedEvents()).hasSize(1);
        assertThat(publisher.lastEvent()).isInstanceOf(OfficerUnlinkedFromCompanyEvent.class);
    }

    @Test
    void managerUnlinksFromAnyCompany() {
        OfficerFullView result = handler.unlink(unlinkCommand(UUID.randomUUID(), Role.MANAGER, UUID.randomUUID()));

        assertThat(result.companyLinks()).allMatch(l -> !l.active());
    }

    @Test
    void adminUnlinksFromAnyCompany() {
        OfficerFullView result = handler.unlink(unlinkCommand(UUID.randomUUID(), Role.ADMIN, UUID.randomUUID()));

        assertThat(result.companyLinks()).allMatch(l -> !l.active());
    }

    @Test
    void nonOwnerUserCannotUnlink() {
        UUID notOwner = UUID.randomUUID();

        assertThatThrownBy(() -> handler.unlink(unlinkCommand(notOwner, Role.USER, ownerId)))
                .isInstanceOf(OfficerAccessDeniedException.class);

        assertThat(publisher.publishedEvents()).isEmpty();
    }

    private UnlinkOfficerFromCompanyUseCase.Command unlinkCommand(UUID callerId, Role role, UUID companyOwnerId) {
        return new UnlinkOfficerFromCompanyUseCase.Command(
                callerId, role, companyOwnerId, seedOfficer.id(), companyId
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
