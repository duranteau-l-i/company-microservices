package com.company.companyservice.unit.application.query;

import com.company.companyservice.application.query.GetCompanyHandler;
import com.company.companyservice.domain.exception.CompanyNotFoundException;
import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.model.CompanyStatus;
import com.company.companyservice.domain.model.OfficerSummary;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.usecases.GetCompanyUseCase;
import com.company.companyservice.stubs.InMemoryCompanyQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetCompanyHandlerTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final CompanyId COMPANY_ID = CompanyId.generate();

    private InMemoryCompanyQueryRepository queryRepo;
    private GetCompanyHandler handler;

    @BeforeEach
    void setUp() {
        queryRepo = new InMemoryCompanyQueryRepository();
        handler = new GetCompanyHandler(queryRepo);
    }

    private CompanyFullView seed(List<OfficerSummary> officers) {
        CompanyFullView seeded = new CompanyFullView(
                COMPANY_ID,
                "Acme Corp",
                "REG-001",
                new Address("1 Main St", "Paris", "75001", "France"),
                OWNER_ID,
                "Alice Smith",
                CompanyStatus.ACTIVE,
                Instant.now(),
                Instant.now(),
                officers
        );
        queryRepo.save(seeded);
        return seeded;
    }

    @Test
    void adminSeesFullView() {
        seed(List.of());
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(UUID.randomUUID(), Role.ADMIN, COMPANY_ID);

        GetCompanyUseCase.Result result = handler.get(query);

        assertThat(result.view()).isInstanceOf(CompanyFullView.class);
    }

    @Test
    void managerSeesFullView() {
        seed(List.of());
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(UUID.randomUUID(), Role.MANAGER, COMPANY_ID);

        GetCompanyUseCase.Result result = handler.get(query);

        assertThat(result.view()).isInstanceOf(CompanyFullView.class);
    }

    @Test
    void ownerSeesFullView() {
        seed(List.of());
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(OWNER_ID, Role.USER, COMPANY_ID);

        GetCompanyUseCase.Result result = handler.get(query);

        assertThat(result.view()).isInstanceOf(CompanyFullView.class);
    }

    @Test
    void managerWhoIsAlsoOwnerSeesFullView() {
        seed(List.of());
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(OWNER_ID, Role.MANAGER, COMPANY_ID);

        GetCompanyUseCase.Result result = handler.get(query);

        assertThat(result.view()).isInstanceOf(CompanyFullView.class);
    }

    @Test
    void nonOwnerSeesRestrictedView() {
        seed(List.of());
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(UUID.randomUUID(), Role.USER, COMPANY_ID);

        GetCompanyUseCase.Result result = handler.get(query);

        assertThat(result.view()).isInstanceOf(CompanyRestrictedView.class);
    }

    @Test
    void fullViewIncludesEmbeddedOfficers() {
        OfficerSummary officer = new OfficerSummary(UUID.randomUUID(), "John", "Doe", "Director");
        seed(List.of(officer));

        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(OWNER_ID, Role.USER, COMPANY_ID);
        GetCompanyUseCase.Result result = handler.get(query);

        assertThat(result.view()).isInstanceOf(CompanyFullView.class);
        CompanyFullView full = (CompanyFullView) result.view();
        assertThat(full.officers()).containsExactly(officer);
    }

    @Test
    void notFound() {
        CompanyId unknownId = CompanyId.generate();
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(UUID.randomUUID(), Role.USER, unknownId);

        assertThatThrownBy(() -> handler.get(query))
                .isInstanceOf(CompanyNotFoundException.class);
    }
}
