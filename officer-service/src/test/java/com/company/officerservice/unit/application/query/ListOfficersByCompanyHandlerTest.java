package com.company.officerservice.unit.application.query;

import com.company.officerservice.application.query.ListOfficersByCompanyHandler;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.usecases.ListOfficersByCompanyUseCase;
import com.company.officerservice.stubs.InMemoryOfficerQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ListOfficersByCompanyHandlerTest {

    private InMemoryOfficerQueryRepository queryRepo;
    private ListOfficersByCompanyHandler handler;

    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        queryRepo = new InMemoryOfficerQueryRepository();
        handler = new ListOfficersByCompanyHandler(queryRepo);

        CompanyLink activeLink = CompanyLink.create(companyId, "Director", LocalDate.of(2024, 1, 1));
        CompanyLink otherLink = CompanyLink.create(UUID.randomUUID(), "CEO", LocalDate.of(2024, 1, 1));

        queryRepo.save(officerWithLinks("Alice", "Smith", List.of(activeLink)));
        queryRepo.save(officerWithLinks("Bob", "Jones", List.of(otherLink)));
        queryRepo.save(officerWithLinks("Carol", "White", List.of()));
    }

    @Test
    void returnsOfficersLinkedToCompany() {
        List<OfficerFullView> results = handler.list(
                new ListOfficersByCompanyUseCase.Command(UUID.randomUUID(), Role.USER, companyId));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).firstName()).isEqualTo("Alice");
    }

    @Test
    void returnsEmptyListForUnknownCompany() {
        List<OfficerFullView> results = handler.list(
                new ListOfficersByCompanyUseCase.Command(UUID.randomUUID(), Role.USER, UUID.randomUUID()));

        assertThat(results).isEmpty();
    }

    private static OfficerFullView officerWithLinks(String firstName, String lastName, List<CompanyLink> links) {
        return new OfficerFullView(
                OfficerId.generate(), firstName, lastName, LocalDate.of(1990, 1, 1),
                "French", null, firstName.toLowerCase() + "@example.com", null,
                links, Instant.now(), Instant.now()
        );
    }
}
