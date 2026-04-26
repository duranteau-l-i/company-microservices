package com.company.companyservice.unit.infrastructure.feign;

import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.port.infrastructure.OfficerQueryPort.OfficerQueryResult;
import com.company.companyservice.infrastructure.feign.OfficerClientAdapter;
import com.company.companyservice.infrastructure.feign.OfficerClientDto;
import com.company.companyservice.infrastructure.feign.OfficerClientFallbackFactory;
import com.company.companyservice.infrastructure.feign.OfficerCompanyLinkDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OfficerClientAdapterTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID officerId = UUID.randomUUID();

    @BeforeEach
    @AfterEach
    void clearThreadLocal() {
        OfficerClientFallbackFactory.FALLBACK_FIRED.remove();
    }

    @Test
    void detectsFallback_viaThreadLocal_whenCircuitBreakerFiresFallback() {
        // Simulates what FeignCircuitBreakerTargeter does: invoke fallback factory,
        // which sets FALLBACK_FIRED and returns List.of() instead of throwing.
        OfficerClientAdapter adapter = new OfficerClientAdapter(id -> {
            OfficerClientFallbackFactory.FALLBACK_FIRED.set(true);
            return List.of();
        });

        OfficerQueryResult result = adapter.findOfficersByCompanyId(CompanyId.of(companyId));

        assertThat(result.fallback()).isTrue();
        assertThat(result.officers()).isEmpty();
    }

    @Test
    void mapsOfficerDto_toOfficerSummary_onSuccessResponse() {
        OfficerClientDto dto = new OfficerClientDto(
                officerId, "Jane", "Smith",
                List.of(new OfficerCompanyLinkDto(companyId, "CEO")));

        OfficerClientAdapter adapter = new OfficerClientAdapter(id -> List.of(dto));

        OfficerQueryResult result = adapter.findOfficersByCompanyId(CompanyId.of(companyId));

        assertThat(result.fallback()).isFalse();
        assertThat(result.officers()).hasSize(1);
        assertThat(result.officers().getFirst().officerId()).isEqualTo(officerId);
        assertThat(result.officers().getFirst().firstName()).isEqualTo("Jane");
        assertThat(result.officers().getFirst().lastName()).isEqualTo("Smith");
        assertThat(result.officers().getFirst().title()).isEqualTo("CEO");
    }

    @Test
    void skipsOfficer_whenNoMatchingCompanyLink() {
        OfficerClientDto dto = new OfficerClientDto(
                officerId, "Jane", "Smith",
                List.of(new OfficerCompanyLinkDto(UUID.randomUUID(), "CFO")));

        OfficerClientAdapter adapter = new OfficerClientAdapter(id -> List.of(dto));

        OfficerQueryResult result = adapter.findOfficersByCompanyId(CompanyId.of(companyId));

        assertThat(result.fallback()).isFalse();
        assertThat(result.officers()).isEmpty();
    }

    @Test
    void returnsFallback_onException_catchPath() {
        OfficerClientAdapter adapter = new OfficerClientAdapter(id -> {
            throw new RuntimeException("circuit breaker not active");
        });

        OfficerQueryResult result = adapter.findOfficersByCompanyId(CompanyId.of(companyId));

        assertThat(result.fallback()).isTrue();
        assertThat(result.officers()).isEmpty();
    }
}
