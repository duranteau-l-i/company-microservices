package com.company.officerservice.unit.application.query;

import com.company.officerservice.application.query.SearchOfficersHandler;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.OfficerRestrictedView;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.usecases.SearchOfficersUseCase;
import com.company.officerservice.stubs.InMemoryOfficerQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SearchOfficersHandlerTest {

    private InMemoryOfficerQueryRepository queryRepo;
    private SearchOfficersHandler handler;

    @BeforeEach
    void setUp() {
        queryRepo = new InMemoryOfficerQueryRepository();
        handler = new SearchOfficersHandler(queryRepo);

        queryRepo.save(officer("Alice", "Smith", LocalDate.of(1990, 1, 15)));
        queryRepo.save(officer("Alice", "Johnson", LocalDate.of(1985, 5, 20)));
        queryRepo.save(officer("Bob", "Smith", LocalDate.of(1990, 1, 15)));
    }

    @Test
    void searchByFirstNameReturnsMatches() {
        List<OfficerRestrictedView> results = handler.search(
                new SearchOfficersUseCase.Command(UUID.randomUUID(), Role.USER, "alice", null, null));

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(v -> v.firstName().equalsIgnoreCase("Alice"));
    }

    @Test
    void searchByLastNameReturnsMatches() {
        List<OfficerRestrictedView> results = handler.search(
                new SearchOfficersUseCase.Command(UUID.randomUUID(), Role.USER, null, "Smith", null));

        assertThat(results).hasSize(2);
    }

    @Test
    void searchByFullNameAndDateOfBirth() {
        List<OfficerRestrictedView> results = handler.search(
                new SearchOfficersUseCase.Command(UUID.randomUUID(), Role.USER, "Alice", "Smith", LocalDate.of(1990, 1, 15)));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).firstName()).isEqualTo("Alice");
        assertThat(results.get(0).lastName()).isEqualTo("Smith");
    }

    @Test
    void searchWithNoFiltersReturnsAll() {
        List<OfficerRestrictedView> results = handler.search(
                new SearchOfficersUseCase.Command(UUID.randomUUID(), Role.USER, null, null, null));

        assertThat(results).hasSize(3);
    }

    @Test
    void searchReturnsRestrictedViews() {
        List<OfficerRestrictedView> results = handler.search(
                new SearchOfficersUseCase.Command(UUID.randomUUID(), Role.USER, "Alice", "Smith", null));

        assertThat(results).allMatch(v -> v instanceof OfficerRestrictedView);
    }

    @Test
    void regexWildcardInputIsNotExpandedToMatchAll() {
        List<OfficerRestrictedView> results = handler.search(
                new SearchOfficersUseCase.Command(UUID.randomUUID(), Role.USER, ".*", null, null));

        assertThat(results).isEmpty();
    }

    private static OfficerFullView officer(String firstName, String lastName, LocalDate dob) {
        return new OfficerFullView(
                OfficerId.generate(), firstName, lastName, dob,
                "French", null, firstName.toLowerCase() + "@example.com", null,
                List.of(), Instant.now(), Instant.now()
        );
    }
}
