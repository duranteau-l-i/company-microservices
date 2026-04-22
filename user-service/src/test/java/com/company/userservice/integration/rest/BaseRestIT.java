package com.company.userservice.integration.rest;

import com.company.userservice.config.UseCaseConfig;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.port.infrastructure.PasswordHasher;
import com.company.userservice.presentation.rest.AuthController;
import com.company.userservice.presentation.rest.UserController;
import com.company.userservice.security.JwtAuthenticationFilter;
import com.company.userservice.security.JwtTokenProvider;
import com.company.userservice.security.SecurityConfig;
import com.company.userservice.stubs.InMemoryUserCommandRepository;
import com.company.userservice.stubs.InMemoryUserEventPublisher;
import com.company.userservice.stubs.InMemoryUserQueryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = {AuthController.class, UserController.class},
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class, UseCaseConfig.class, RestTestConfig.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=integration-test-secret-that-is-at-least-64-bytes-long-xxxxxxxxxxxxxxxxx",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
abstract class BaseRestIT {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JwtTokenProvider jwtTokenProvider;
    @Autowired protected InMemoryUserCommandRepository commandRepo;
    @Autowired protected InMemoryUserQueryRepository queryRepo;
    @Autowired protected InMemoryUserEventPublisher eventPublisher;
    @Autowired protected PasswordHasher passwordHasher;

    @BeforeEach
    void clearStubs() {
        commandRepo.clear();
        queryRepo.clear();
        eventPublisher.clear();
    }

    protected String token(UserId id, String email, Role role) {
        return "Bearer " + jwtTokenProvider.issueTokens(id, email, role).accessToken();
    }
}