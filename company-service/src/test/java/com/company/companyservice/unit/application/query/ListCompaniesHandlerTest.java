package com.company.companyservice.unit.application.query;

import com.company.companyservice.application.query.ListCompaniesHandler;
import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyStatus;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.usecases.ListCompaniesUseCase;
import com.company.companyservice.unit.application.stubs.InMemoryCompanyQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ListCompaniesHandlerTest {

    private static final UUID OWNER_A = UUID.randomUUID();
    private static final UUID OWNER_B = UUID.randomUUID();

    private InMemoryCompanyQueryRepository queryRepo;
    private ListCompaniesHandler handler;

    @BeforeEach
    void setUp() {
        queryRepo = new InMemoryCompanyQueryRepository();
        handler = new ListCompaniesHandler(queryRepo);

        queryRepo.save(new CompanyFullView(
                CompanyId.generate(),
                "Company A",
                "REG-A",
                new Address("1 A St", "Paris", "75001", "France"),
                OWNER_A,
                "Alice",
                CompanyStatus.ACTIVE,
                Instant.now(),
                Instant.now(),
                List.of()
        ));
        queryRepo.save(new CompanyFullView(
                CompanyId.generate(),
                "Company B",
                "REG-B",
                new Address("2 B St", "Lyon", "69001", "France"),
                OWNER_B,
                "Bob",
                CompanyStatus.ACTIVE,
                Instant.now(),
                Instant.now(),
                List.of()
        ));
    }

    @Test
    void adminListsAll() {
        ListCompaniesUseCase.Query query = new ListCompaniesUseCase.Query(UUID.randomUUID(), Role.ADMIN);

        List<CompanyFullView> result = handler.list(query);

        assertThat(result).hasSize(2);
    }

    @Test
    void managerListsAll() {
        ListCompaniesUseCase.Query query = new ListCompaniesUseCase.Query(UUID.randomUUID(), Role.MANAGER);

        List<CompanyFullView> result = handler.list(query);

        assertThat(result).hasSize(2);
    }

    @Test
    void userListsOwnOnly() {
        ListCompaniesUseCase.Query query = new ListCompaniesUseCase.Query(OWNER_A, Role.USER);

        List<CompanyFullView> result = handler.list(query);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ownerId()).isEqualTo(OWNER_A);
    }

    @Test
    void emptyForUserWithNoCompanies() {
        UUID noCompaniesUser = UUID.randomUUID();
        ListCompaniesUseCase.Query query = new ListCompaniesUseCase.Query(noCompaniesUser, Role.USER);

        List<CompanyFullView> result = handler.list(query);

        assertThat(result).isEmpty();
    }
}
