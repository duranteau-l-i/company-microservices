package com.company.userservice.integration.persistence;

import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.infrastructure.persistence.command.PostgresUserCommandRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresUserCommandRepository.class)
@Testcontainers
class PostgresUserCommandRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.flyway.url", postgres::getJdbcUrl);
        r.add("spring.flyway.user", postgres::getUsername);
        r.add("spring.flyway.password", postgres::getPassword);
        r.add("spring.cloud.config.enabled", () -> "false");
        r.add("spring.cloud.discovery.enabled", () -> "false");
        r.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    PostgresUserCommandRepository repository;

    @Test
    void saveAndFindById() {
        User user = User.create(EmailAddress.of("ita@test.com"), "h", "Ita", "Test", Role.USER).user();
        repository.save(user);

        Optional<User> found = repository.findById(user.id());

        assertThat(found).isPresent();
        assertThat(found.get().email().value()).isEqualTo("ita@test.com");
    }

    @Test
    void findByEmail() {
        User user = User.create(EmailAddress.of("find@test.com"), "h", "F", "T", Role.USER).user();
        repository.save(user);

        assertThat(repository.findByEmail(EmailAddress.of("find@test.com"))).isPresent();
        assertThat(repository.existsByEmail(EmailAddress.of("find@test.com"))).isTrue();
        assertThat(repository.existsByEmail(EmailAddress.of("none@test.com"))).isFalse();
    }

    @Test
    void deleteRemovesUser() {
        User user = User.create(EmailAddress.of("del@test.com"), "h", "D", "T", Role.USER).user();
        repository.save(user);

        repository.delete(user.id());

        assertThat(repository.findById(user.id())).isEmpty();
    }

    @Test
    void returnsEmptyForUnknownId() {
        assertThat(repository.findById(UserId.generate())).isEmpty();
    }
}
