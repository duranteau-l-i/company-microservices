package com.company.officerservice.unit.application.command;

import com.company.officerservice.application.command.DeleteOfficerHandler;
import com.company.officerservice.domain.event.OfficerDeletedEvent;
import com.company.officerservice.domain.exception.OfficerAccessDeniedException;
import com.company.officerservice.domain.exception.OfficerNotFoundException;
import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.usecases.DeleteOfficerUseCase;
import com.company.officerservice.unit.application.stubs.InMemoryOfficerCommandRepository;
import com.company.officerservice.unit.application.stubs.InMemoryOfficerEventPublisher;
import com.company.officerservice.unit.application.stubs.InMemoryOfficerQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeleteOfficerHandlerTest {

    private InMemoryOfficerCommandRepository commandRepo;
    private InMemoryOfficerQueryRepository queryRepo;
    private InMemoryOfficerEventPublisher publisher;
    private DeleteOfficerHandler handler;

    private Officer seedOfficer;

    @BeforeEach
    void setUp() {
        commandRepo = new InMemoryOfficerCommandRepository();
        queryRepo = new InMemoryOfficerQueryRepository();
        publisher = new InMemoryOfficerEventPublisher();
        handler = new DeleteOfficerHandler(commandRepo, queryRepo, publisher);

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
    void adminDeletesOfficer() {
        handler.delete(new DeleteOfficerUseCase.Command(UUID.randomUUID(), Role.ADMIN, seedOfficer.id()));

        assertThat(commandRepo.size()).isEqualTo(0);
        assertThat(queryRepo.size()).isEqualTo(0);
        assertThat(publisher.publishedEvents()).hasSize(1);
        assertThat(publisher.lastEvent()).isInstanceOf(OfficerDeletedEvent.class);
    }

    @Test
    void managerCannotDelete() {
        assertThatThrownBy(() ->
                handler.delete(new DeleteOfficerUseCase.Command(UUID.randomUUID(), Role.MANAGER, seedOfficer.id())))
                .isInstanceOf(OfficerAccessDeniedException.class);

        assertThat(commandRepo.size()).isEqualTo(1);
        assertThat(publisher.publishedEvents()).isEmpty();
    }

    @Test
    void userCannotDelete() {
        assertThatThrownBy(() ->
                handler.delete(new DeleteOfficerUseCase.Command(UUID.randomUUID(), Role.USER, seedOfficer.id())))
                .isInstanceOf(OfficerAccessDeniedException.class);

        assertThat(commandRepo.size()).isEqualTo(1);
        assertThat(publisher.publishedEvents()).isEmpty();
    }

    @Test
    void officerNotFoundThrows() {
        assertThatThrownBy(() ->
                handler.delete(new DeleteOfficerUseCase.Command(UUID.randomUUID(), Role.ADMIN, OfficerId.generate())))
                .isInstanceOf(OfficerNotFoundException.class);
    }

    private static OfficerFullView toFullView(Officer officer) {
        return new OfficerFullView(
                officer.id(), officer.firstName(), officer.lastName(), officer.dateOfBirth(),
                officer.nationality(), officer.address(), officer.email(), officer.phone(),
                officer.companyLinks(), officer.createdAt(), officer.updatedAt()
        );
    }
}
