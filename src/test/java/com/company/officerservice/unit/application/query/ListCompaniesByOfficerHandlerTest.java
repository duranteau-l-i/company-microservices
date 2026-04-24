package com.company.officerservice.unit.application.query;

import com.company.officerservice.application.query.ListCompaniesByOfficerHandler;
import com.company.officerservice.domain.exception.OfficerNotFoundException;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.usecases.ListCompaniesByOfficerUseCase;
import com.company.officerservice.unit.application.stubs.InMemoryOfficerQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListCompaniesByOfficerHandlerTest {

    private InMemoryOfficerQueryRepository queryRepo;
    private ListCompaniesByOfficerHandler handler;

    private OfficerFullView seedView;

    @BeforeEach
    void setUp() {
        queryRepo = new InMemoryOfficerQueryRepository();
        handler = new ListCompaniesByOfficerHandler(queryRepo);

        CompanyLink link1 = CompanyLink.create(UUID.randomUUID(), "Director", LocalDate.of(2024, 1, 1));
        CompanyLink link2 = CompanyLink.create(UUID.randomUUID(), "CEO", LocalDate.of(2023, 6, 1));

        seedView = new OfficerFullView(
                OfficerId.generate(), "Alice", "Smith", LocalDate.of(1990, 1, 15),
                "French", null, "alice@example.com", null,
                List.of(link1, link2), Instant.now(), Instant.now()
        );
        queryRepo.save(seedView);
    }

    @Test
    void returnsOfficerWithAllCompanyLinks() {
        OfficerFullView result = handler.list(
                new ListCompaniesByOfficerUseCase.Command(UUID.randomUUID(), Role.USER, seedView.id()));

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(seedView.id());
        assertThat(result.companyLinks()).hasSize(2);
    }

    @Test
    void officerNotFoundThrows() {
        assertThatThrownBy(() ->
                handler.list(new ListCompaniesByOfficerUseCase.Command(UUID.randomUUID(), Role.USER, OfficerId.generate())))
                .isInstanceOf(OfficerNotFoundException.class);
    }
}
