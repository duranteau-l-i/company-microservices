package com.company.officerservice.integration.controller;

import com.company.officerservice.stubs.InMemoryOfficerCommandRepository;
import com.company.officerservice.stubs.InMemoryOfficerEventPublisher;
import com.company.officerservice.stubs.InMemoryOfficerQueryRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
class RestTestConfig {

    @Bean
    InMemoryOfficerCommandRepository officerCommandRepository() {
        return new InMemoryOfficerCommandRepository();
    }

    @Bean
    InMemoryOfficerQueryRepository officerQueryRepository() {
        return new InMemoryOfficerQueryRepository();
    }

    @Bean
    InMemoryOfficerEventPublisher officerEventPublisher() {
        return new InMemoryOfficerEventPublisher();
    }
}
