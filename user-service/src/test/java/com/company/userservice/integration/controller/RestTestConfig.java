package com.company.userservice.integration.controller;

import com.company.userservice.domain.port.infrastructure.PasswordHasher;
import com.company.userservice.stubs.InMemoryPasswordHasher;
import com.company.userservice.stubs.InMemoryRefreshTokenRepository;
import com.company.userservice.stubs.InMemoryUserCommandRepository;
import com.company.userservice.stubs.InMemoryUserEventPublisher;
import com.company.userservice.stubs.InMemoryUserQueryRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
class RestTestConfig {

    @Bean
    InMemoryUserCommandRepository commandRepository() {
        return new InMemoryUserCommandRepository();
    }

    @Bean
    InMemoryRefreshTokenRepository refreshTokenRepository() {
        return new InMemoryRefreshTokenRepository();
    }

    @Bean
    InMemoryUserQueryRepository queryRepository() {
        return new InMemoryUserQueryRepository();
    }

    @Bean
    InMemoryUserEventPublisher eventPublisher() {
        return new InMemoryUserEventPublisher();
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new InMemoryPasswordHasher();
    }
}
