package com.company.companyservice.integration.rest;

import com.company.companyservice.unit.application.stubs.InMemoryCompanyCommandRepository;
import com.company.companyservice.unit.application.stubs.InMemoryCompanyEventPublisher;
import com.company.companyservice.unit.application.stubs.InMemoryCompanyQueryRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
class RestTestConfig {

    @Bean
    InMemoryCompanyCommandRepository companyCommandRepository() {
        return new InMemoryCompanyCommandRepository();
    }

    @Bean
    InMemoryCompanyQueryRepository companyQueryRepository() {
        return new InMemoryCompanyQueryRepository();
    }

    @Bean
    InMemoryCompanyEventPublisher companyEventPublisher() {
        return new InMemoryCompanyEventPublisher();
    }
}
