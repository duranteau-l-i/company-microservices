package com.company.companyservice.unit.infrastructure.feign;

import com.company.companyservice.infrastructure.feign.OfficerClient;
import com.company.companyservice.infrastructure.feign.OfficerClientDto;
import com.company.companyservice.infrastructure.feign.OfficerClientFallbackFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OfficerClientFallbackFactoryTest {

    @BeforeEach
    @AfterEach
    void clearThreadLocal() {
        OfficerClientFallbackFactory.FALLBACK_FIRED.remove();
    }

    @Test
    void create_setsThreadLocalAndReturnsEmptyList() {
        OfficerClientFallbackFactory factory = new OfficerClientFallbackFactory();
        OfficerClient fallback = factory.create(new RuntimeException("service down"));

        List<OfficerClientDto> result = fallback.getOfficersByCompanyId(UUID.randomUUID());

        assertThat(result).isEmpty();
        assertThat(OfficerClientFallbackFactory.FALLBACK_FIRED.get()).isTrue();
    }

    @Test
    void create_doesNotLeakThreadLocal_acrossFactoryInstances() {
        OfficerClientFallbackFactory factory = new OfficerClientFallbackFactory();
        OfficerClient fallback = factory.create(new RuntimeException("first call"));
        fallback.getOfficersByCompanyId(UUID.randomUUID());
        OfficerClientFallbackFactory.FALLBACK_FIRED.remove();

        assertThat(OfficerClientFallbackFactory.FALLBACK_FIRED.get()).isNull();
    }
}
