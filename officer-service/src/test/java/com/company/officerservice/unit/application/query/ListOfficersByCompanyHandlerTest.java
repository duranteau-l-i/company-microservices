package com.company.officerservice.unit.application.query;

import com.company.officerservice.application.query.ListOfficersByCompanyHandler;
import com.company.officerservice.domain.exception.OfficerAccessDeniedException;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.OfficerRestrictedView;
import com.company.officerservice.domain.model.OfficerView;
import com.company.officerservice.domain.model.Role;
import com.company.officerservice.domain.port.usecases.ListOfficersByCompanyUseCase;
import com.company.officerservice.stubs.InMemoryCompanyValidationPort;
import com.company.officerservice.stubs.InMemoryOfficerQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListOfficersByCompanyHandlerTest {

    private InMemoryOfficerQueryRepository queryRepo;
    private InMemoryCompanyValidationPort companyValidationPort;
    private ListOfficersByCompanyHandler handler;

    private final UUID companyId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        queryRepo = new InMemoryOfficerQueryRepository();
        companyValidationPort = new InMemoryCompanyValidationPort();
        companyValidationPort.addCompany(companyId, ownerId);
        handler = new ListOfficersByCompanyHandler(queryRepo, companyValidationPort);

        CompanyLink activeLink = CompanyLink.create(companyId, "Director", LocalDate.of(2024, 1, 1));
        CompanyLink otherLink = CompanyLink.create(UUID.randomUUID(), "CEO", LocalDate.of(2024, 1, 1));

        queryRepo.save(officerWithLinks("Alice", "Smith", List.of(activeLink)));
        queryRepo.save(officerWithLinks("Bob", "Jones", List.of(otherLink)));
        queryRepo.save(officerWithLinks("Carol", "White", List.of()));
    }

    @Test
    void managerReceivesFullViewWithPii() {
        List<OfficerView> results = handler.list(
                new ListOfficersByCompanyUseCase.Command(UUID.randomUUID(), Role.MANAGER, companyId));

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isInstanceOf(OfficerFullView.class);
        assertThat(((OfficerFullView) results.get(0)).email()).isEqualTo("alice@example.com");
    }

    @Test
    void ownerUserReceivesRestrictedView() {
        List<OfficerView> results = handler.list(
                new ListOfficersByCompanyUseCase.Command(ownerId, Role.USER, companyId));

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isInstanceOf(OfficerRestrictedView.class);
    }

    @Test
    void nonOwnerUserIsRejected() {
        assertThatThrownBy(() -> handler.list(
                new ListOfficersByCompanyUseCase.Command(UUID.randomUUID(), Role.USER, companyId)))
                .isInstanceOf(OfficerAccessDeniedException.class);
    }

    @Test
    void returnsEmptyListForUnknownCompanyAsManager() {
        List<OfficerView> results = handler.list(
                new ListOfficersByCompanyUseCase.Command(UUID.randomUUID(), Role.MANAGER, UUID.randomUUID()));

        assertThat(results).isEmpty();
    }

    @Test
    void userIsRejectedWhenCompanyNotInProjection() {
        companyValidationPort.clear();

        assertThatThrownBy(() -> handler.list(
                new ListOfficersByCompanyUseCase.Command(UUID.randomUUID(), Role.USER, companyId)))
                .isInstanceOf(OfficerAccessDeniedException.class);
    }

    private static OfficerFullView officerWithLinks(String firstName, String lastName, List<CompanyLink> links) {
        return new OfficerFullView(
                OfficerId.generate(), firstName, lastName, LocalDate.of(1990, 1, 1),
                "French", null, firstName.toLowerCase() + "@example.com", null,
                links, Instant.now(), Instant.now()
        );
    }
}
