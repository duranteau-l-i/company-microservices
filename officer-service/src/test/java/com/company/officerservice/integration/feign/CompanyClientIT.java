package com.company.officerservice.integration.feign;

import com.company.officerservice.domain.exception.ServiceUnavailableException;
import com.company.officerservice.infrastructure.feign.CompanyClient;
import com.company.officerservice.infrastructure.feign.CompanyClientAdapter;
import com.company.officerservice.infrastructure.feign.CompanyClientFallbackFactory;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(CompanyClientIT.TestConfig.class)
class CompanyClientIT {

    static WireMockServer wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    static {
        wireMock.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.openfeign.client.config.company-service.url",
                () -> "http://localhost:" + wireMock.port());
        registry.add("spring.cloud.openfeign.client.config.company-service.readTimeout", () -> "500");
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @Configuration
    @EnableFeignClients(clients = CompanyClient.class)
    @ImportAutoConfiguration({FeignAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class})
    static class TestConfig {

        @Bean
        CompanyClientFallbackFactory companyClientFallbackFactory() {
            return new CompanyClientFallbackFactory();
        }

        @Bean
        CompanyClientAdapter companyClientAdapter(CompanyClient companyClient) {
            // findOwnerId reads from the known_companies projection (not Feign); this
            // WireMock-based IT only exercises companyExists, so the repo is unused here.
            return new CompanyClientAdapter(companyClient, null);
        }
    }

    @Autowired
    private CompanyClientAdapter companyClientAdapter;

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Test
    void companyExists_returnsTrue_whenCompanyFound() {
        UUID companyId = UUID.randomUUID();

        wireMock.stubFor(get(urlPathEqualTo("/api/companies/" + companyId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": "%s", "name": "Acme Corp"}
                                """.formatted(companyId))));

        assertThat(companyClientAdapter.companyExists(companyId)).isTrue();
    }

    @Test
    void companyExists_returnsFalse_whenCompanyNotFound() {
        UUID companyId = UUID.randomUUID();

        wireMock.stubFor(get(urlPathEqualTo("/api/companies/" + companyId))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Not Found")));

        assertThat(companyClientAdapter.companyExists(companyId)).isFalse();
    }

    @Test
    void companyExists_throwsServiceUnavailable_whenCompanyServiceDown() {
        UUID companyId = UUID.randomUUID();

        wireMock.stubFor(get(urlPathEqualTo("/api/companies/" + companyId))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        assertThatThrownBy(() -> companyClientAdapter.companyExists(companyId))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Cannot verify company");
    }

    @Test
    void companyExists_throwsServiceUnavailable_whenCompanyServiceTimesOut() {
        UUID companyId = UUID.randomUUID();

        wireMock.stubFor(get(urlPathEqualTo("/api/companies/" + companyId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")
                        .withFixedDelay(2000)));

        assertThatThrownBy(() -> companyClientAdapter.companyExists(companyId))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Cannot verify company");
    }
}
