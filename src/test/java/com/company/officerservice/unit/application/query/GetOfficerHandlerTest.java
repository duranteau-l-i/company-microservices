package com.company.officerservice.unit.application.query;

import com.company.officerservice.application.query.GetOfficerHandler;
import com.company.officerservice.domain.exception.OfficerNotFoundException;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.OfficerRestrictedView;
import com.company.officerservice.domain.model.OfficerView;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.usecases.GetOfficerUseCase;
import com.company.officerservice.stubs.InMemoryOfficerQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetOfficerHandlerTest {

    private InMemoryOfficerQueryRepository queryRepo;
    private GetOfficerHandler handler;

    private OfficerFullView seedView;

    @BeforeEach
    void setUp() {
        queryRepo = new InMemoryOfficerQueryRepository();
        handler = new GetOfficerHandler(queryRepo);

        seedView = new OfficerFullView(
                OfficerId.generate(), "Alice", "Smith", LocalDate.of(1990, 1, 15),
                "French", null, "alice@example.com", null,
                List.of(), Instant.now(), Instant.now()
        );
        queryRepo.save(seedView);
    }

    @Test
    void adminSeesFullView() {
        OfficerView result = handler.get(new GetOfficerUseCase.Command(UUID.randomUUID(), Role.ADMIN, seedView.id()));

        assertThat(result).isInstanceOf(OfficerFullView.class);
        OfficerFullView full = (OfficerFullView) result;
        assertThat(full.email()).isEqualTo("alice@example.com");
        assertThat(full.nationality()).isEqualTo("French");
    }

    @Test
    void managerSeesFullView() {
        OfficerView result = handler.get(new GetOfficerUseCase.Command(UUID.randomUUID(), Role.MANAGER, seedView.id()));

        assertThat(result).isInstanceOf(OfficerFullView.class);
    }

    @Test
    void userSeesRestrictedView() {
        OfficerView result = handler.get(new GetOfficerUseCase.Command(UUID.randomUUID(), Role.USER, seedView.id()));

        assertThat(result).isInstanceOf(OfficerRestrictedView.class);
        OfficerRestrictedView restricted = (OfficerRestrictedView) result;
        assertThat(restricted.firstName()).isEqualTo("Alice");
        assertThat(restricted.lastName()).isEqualTo("Smith");
    }

    @Test
    void officerNotFoundThrows() {
        assertThatThrownBy(() ->
                handler.get(new GetOfficerUseCase.Command(UUID.randomUUID(), Role.ADMIN, OfficerId.generate())))
                .isInstanceOf(OfficerNotFoundException.class);
    }
}
