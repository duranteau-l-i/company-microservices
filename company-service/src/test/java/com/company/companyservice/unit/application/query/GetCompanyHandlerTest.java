package com.company.companyservice.unit.application.query;

import com.company.companyservice.application.query.GetCompanyHandler;
import com.company.companyservice.domain.exception.CompanyNotFoundException;
import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.model.CompanyStatus;
import com.company.companyservice.domain.model.CompanyView;
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
                List.of()
        );
        queryRepo.save(seeded);
    }

    @Test
    void adminSeesFullView() {
        UUID callerId = UUID.randomUUID();
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(callerId, Role.ADMIN, COMPANY_ID);

        CompanyView result = handler.get(query);

        assertThat(result).isInstanceOf(CompanyFullView.class);
    }

    @Test
    void managerSeesFullView() {
        UUID callerId = UUID.randomUUID();
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(callerId, Role.MANAGER, COMPANY_ID);

        CompanyView result = handler.get(query);

        assertThat(result).isInstanceOf(CompanyFullView.class);
    }

    @Test
    void ownerSeesFullView() {
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(OWNER_ID, Role.USER, COMPANY_ID);

        CompanyView result = handler.get(query);

        assertThat(result).isInstanceOf(CompanyFullView.class);
    }

    @Test
    void nonOwnerSeesRestrictedView() {
        UUID otherUserId = UUID.randomUUID();
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(otherUserId, Role.USER, COMPANY_ID);

        CompanyView result = handler.get(query);

        assertThat(result).isInstanceOf(CompanyRestrictedView.class);
    }

    @Test
    void notFound() {
        CompanyId unknownId = CompanyId.generate();
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(UUID.randomUUID(), Role.USER, unknownId);

        assertThatThrownBy(() -> handler.get(query))
                .isInstanceOf(CompanyNotFoundException.class);
    }
}
