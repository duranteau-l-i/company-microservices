package com.company.officerservice.unit.infrastructure.feign;

import com.company.officerservice.domain.exception.ServiceUnavailableException;
import com.company.officerservice.infrastructure.feign.CompanyClientAdapter;
import com.company.officerservice.infrastructure.feign.CompanyClientDto;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyClientAdapterTest {

    // knownCompanies is only used by findOwnerId (a trivial projection lookup),
    // not by companyExists — these tests exercise the Feign companyExists path only.
    private static CompanyClientAdapter adapterWith(com.company.officerservice.infrastructure.feign.CompanyClient client) {
        return new CompanyClientAdapter(client, null);
    }

    private static FeignException.NotFound notFoundException() {
        Request request = Request.create(
                Request.HttpMethod.GET, "http://localhost/api/companies/test",
                Map.<String, Collection<String>>of(), new byte[0], StandardCharsets.UTF_8, null);
        return new FeignException.NotFound("404", request, null, Map.of());
    }

    @Test
    void companyExists_returnsTrue_whenClientReturnsDto() {
        UUID companyId = UUID.randomUUID();
        CompanyClientAdapter adapter = adapterWith(id -> new CompanyClientDto(id));

        assertThat(adapter.companyExists(companyId)).isTrue();
    }

    @Test
    void companyExists_returnsFalse_whenClientThrowsNotFound() {
        UUID companyId = UUID.randomUUID();
        CompanyClientAdapter adapter = adapterWith(id -> {
            throw notFoundException();
        });

        assertThat(adapter.companyExists(companyId)).isFalse();
    }

    @Test
    void companyExists_rethrows_whenFallbackThrowsServiceUnavailable() {
        UUID companyId = UUID.randomUUID();
        CompanyClientAdapter adapter = adapterWith(id -> {
            throw new ServiceUnavailableException("Cannot verify company — try again later");
        });

        assertThatThrownBy(() -> adapter.companyExists(companyId))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Cannot verify company");
    }

    @Test
    void companyExists_wrapsUnexpectedException_intoServiceUnavailable() {
        UUID companyId = UUID.randomUUID();
        CompanyClientAdapter adapter = adapterWith(id -> {
            throw new RuntimeException("unexpected failure");
        });

        assertThatThrownBy(() -> adapter.companyExists(companyId))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Cannot verify company");
    }
}
