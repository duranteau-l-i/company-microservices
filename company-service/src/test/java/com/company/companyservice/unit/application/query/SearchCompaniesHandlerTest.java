package com.company.companyservice.unit.application.query;

import com.company.companyservice.application.query.SearchCompaniesHandler;
import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.model.CompanyStatus;
import com.company.companyservice.domain.port.usecases.SearchCompaniesUseCase;
import com.company.companyservice.stubs.InMemoryCompanyQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SearchCompaniesHandlerTest {

    private InMemoryCompanyQueryRepository queryRepo;
    private SearchCompaniesHandler handler;

    @BeforeEach
    void setUp() {
        queryRepo = new InMemoryCompanyQueryRepository();
        handler = new SearchCompaniesHandler(queryRepo);

        queryRepo.save(new CompanyFullView(
                CompanyId.generate(),
                "Acme Corp",
                "REG-001",
                new Address("1 Main St", "Paris", "75001", "France"),
                UUID.randomUUID(),
                "Alice",
                CompanyStatus.ACTIVE,
                Instant.now(),
                Instant.now(),
                List.of()
        ));
        queryRepo.save(new CompanyFullView(
                CompanyId.generate(),
                "Beta Industries",
                "REG-002",
                new Address("2 Beta Rd", "Lyon", "69001", "France"),
                UUID.randomUUID(),
                "Bob",
                CompanyStatus.ACTIVE,
                Instant.now(),
                Instant.now(),
                List.of()
        ));
    }

    @Test
    void searchByPartialName() {
        SearchCompaniesUseCase.Query query = new SearchCompaniesUseCase.Query("acme");

        List<CompanyRestrictedView> result = handler.search(query);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Acme Corp");
    }

    @Test
    void searchNoMatch() {
        SearchCompaniesUseCase.Query query = new SearchCompaniesUseCase.Query("zzz");

        List<CompanyRestrictedView> result = handler.search(query);

        assertThat(result).isEmpty();
    }

    @Test
    void searchEmptyTerm() {
        // InMemory: empty string matches everything (contains("") is always true)
        SearchCompaniesUseCase.Query query = new SearchCompaniesUseCase.Query("");

        List<CompanyRestrictedView> result = handler.search(query);

        assertThat(result).hasSize(2);
    }
}
