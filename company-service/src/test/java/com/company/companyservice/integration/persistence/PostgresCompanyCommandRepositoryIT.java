package com.company.companyservice.integration.persistence;

import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.Company;
import com.company.companyservice.infrastructure.persistence.command.PostgresCompanyCommandRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresCompanyCommandRepository.class)
@Testcontainers
class PostgresCompanyCommandRepositoryIT {

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
    PostgresCompanyCommandRepository repository;

    @Autowired
    EntityManager entityManager;

    private Company buildCompany(String regNum) {
        return Company.create(
                "Acme Corp",
                regNum,
                new Address("1 Main St", "Paris", "75001", "France"),
                UUID.randomUUID()
        ).company();
    }

    @Test
    void saveAndFindById() {
        Company company = buildCompany("REG-001");
        repository.save(company);

        var found = repository.findById(company.id());

        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Acme Corp");
        assertThat(found.get().registrationNumber()).isEqualTo("REG-001");
        assertThat(found.get().address().city()).isEqualTo("Paris");
        assertThat(found.get().status().name()).isEqualTo("ACTIVE");
    }

    @Test
    void findByIdReturnsEmptyForUnknown() {
        var found = repository.findById(com.company.companyservice.domain.model.CompanyId.generate());

        assertThat(found).isEmpty();
    }

    @Test
    void existsByRegistrationNumber() {
        Company company = buildCompany("REG-002");
        repository.save(company);

        assertThat(repository.existsByRegistrationNumber("REG-002")).isTrue();
        assertThat(repository.existsByRegistrationNumber("REG-UNKNOWN")).isFalse();
    }

    @Test
    void uniqueRegistrationNumberConstraint() {
        Company first = buildCompany("REG-003");
        repository.save(first);
        entityManager.flush();

        Company duplicate = buildCompany("REG-003");

        assertThatThrownBy(() -> {
            repository.save(duplicate);
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, ConstraintViolationException.class);
    }

    @Test
    void deleteRemovesCompany() {
        Company company = buildCompany("REG-004");
        repository.save(company);

        repository.delete(company.id());

        assertThat(repository.findById(company.id())).isEmpty();
    }
}
