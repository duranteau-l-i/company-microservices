package com.company.companyservice.integration.controller;

import com.company.companyservice.stubs.InMemoryCompanyCommandRepository;
import com.company.companyservice.stubs.InMemoryCompanyEventPublisher;
import com.company.companyservice.stubs.InMemoryCompanyQueryRepository;
import com.company.companyservice.stubs.InMemoryOfficerQueryPort;
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

    @Bean
    InMemoryOfficerQueryPort officerQueryPort() {
        return new InMemoryOfficerQueryPort();
    }
}
