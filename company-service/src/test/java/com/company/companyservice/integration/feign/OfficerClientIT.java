package com.company.companyservice.integration.feign;

import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.OfficerSummary;
import com.company.companyservice.domain.port.infrastructure.OfficerQueryPort;
import com.company.companyservice.infrastructure.feign.OfficerClient;
import com.company.companyservice.infrastructure.feign.OfficerClientAdapter;
import com.company.companyservice.infrastructure.feign.OfficerClientFallbackFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(OfficerClientIT.TestConfig.class)
class OfficerClientIT {

    static WireMockServer wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    static {
        wireMock.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.openfeign.client.config.officer-service.url",
                () -> "http://localhost:" + wireMock.port());
        registry.add("feign.circuitbreaker.enabled", () -> "true");
        registry.add("resilience4j.circuitbreaker.instances.officer-service.sliding-window-size", () -> "10");
        registry.add("resilience4j.circuitbreaker.instances.officer-service.failure-rate-threshold", () -> "50");
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @Configuration
    @EnableFeignClients(clients = OfficerClient.class)
    @ImportAutoConfiguration({FeignAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class})
    static class TestConfig {

        @Bean
        OfficerClientFallbackFactory officerClientFallbackFactory() {
            return new OfficerClientFallbackFactory();
        }

        @Bean
        OfficerClientAdapter officerClientAdapter(OfficerClient officerClient) {
            return new OfficerClientAdapter(officerClient);
        }
    }

    @Autowired
    private OfficerClientAdapter officerClientAdapter;

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Test
    void officerServiceRespondsNormally_officersIncluded() {
        UUID companyId = UUID.randomUUID();
        UUID officerId = UUID.randomUUID();

        wireMock.stubFor(get(urlPathEqualTo("/api/officers/by-company/" + companyId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {
                                    "id": "%s",
                                    "firstName": "John",
                                    "lastName": "Doe",
                                    "companyLinks": [
                                      {
                                        "companyId": "%s",
                                        "title": "Director"
                                      }
                                    ]
                                  }
                                ]
                                """.formatted(officerId, companyId))));

        OfficerQueryPort.OfficerQueryResult result =
                officerClientAdapter.findOfficersByCompanyId(CompanyId.of(companyId));

        assertThat(result.fallback()).isFalse();
        assertThat(result.officers()).hasSize(1);
        OfficerSummary officer = result.officers().getFirst();
        assertThat(officer.officerId()).isEqualTo(officerId);
        assertThat(officer.firstName()).isEqualTo("John");
        assertThat(officer.lastName()).isEqualTo("Doe");
        assertThat(officer.title()).isEqualTo("Director");
    }

    @Test
    void officerServiceReturns500_fallbackActivated() {
        UUID companyId = UUID.randomUUID();

        wireMock.stubFor(get(urlPathEqualTo("/api/officers/by-company/" + companyId))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        OfficerQueryPort.OfficerQueryResult result =
                officerClientAdapter.findOfficersByCompanyId(CompanyId.of(companyId));

        assertThat(result.fallback()).isTrue();
        assertThat(result.officers()).isEmpty();
    }

    @Test
    void officerServiceReturnsEmptyList_noFallback() {
        UUID companyId = UUID.randomUUID();

        wireMock.stubFor(get(urlPathEqualTo("/api/officers/by-company/" + companyId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        OfficerQueryPort.OfficerQueryResult result =
                officerClientAdapter.findOfficersByCompanyId(CompanyId.of(companyId));

        assertThat(result.fallback()).isFalse();
        assertThat(result.officers()).isEmpty();
    }
}
