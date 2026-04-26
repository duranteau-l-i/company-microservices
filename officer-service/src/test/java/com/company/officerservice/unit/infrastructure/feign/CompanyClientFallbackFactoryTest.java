package com.company.officerservice.unit.infrastructure.feign;

import com.company.officerservice.domain.exception.ServiceUnavailableException;
import com.company.officerservice.infrastructure.feign.CompanyClient;
import com.company.officerservice.infrastructure.feign.CompanyClientFallbackFactory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyClientFallbackFactoryTest {

    @Test
    void create_throwsServiceUnavailableException_onFallback() {
        CompanyClientFallbackFactory factory = new CompanyClientFallbackFactory();
        CompanyClient fallback = factory.create(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> fallback.getCompany(UUID.randomUUID()))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Cannot verify company");
    }
}
