package com.company.officerservice.integration.persistence;

import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.infrastructure.persistence.command.PostgresOfficerCommandRepository;
import jakarta.persistence.EntityManager;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresOfficerCommandRepository.class)
@Testcontainers
class PostgresOfficerCommandRepositoryIT {

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
    PostgresOfficerCommandRepository repository;

    @Autowired
    EntityManager entityManager;

    private Officer buildOfficer(String email) {
        return Officer.create(
                "Alice", "Smith", LocalDate.of(1990, 1, 15),
                "French", new Address("1 Rue", "Paris", "75001", "France"),
                email, null
        ).officer();
    }

    @Test
    void saveAndFindById() {
        Officer officer = buildOfficer("alice@example.com");
        repository.save(officer);

        var found = repository.findById(officer.id());

        assertThat(found).isPresent();
        assertThat(found.get().firstName()).isEqualTo("Alice");
        assertThat(found.get().email()).isEqualTo("alice@example.com");
        assertThat(found.get().address().city()).isEqualTo("Paris");
        assertThat(found.get().companyLinks()).isEmpty();
    }

    @Test
    void findByIdReturnsEmptyForUnknown() {
        assertThat(repository.findById(com.company.officerservice.domain.model.OfficerId.generate())).isEmpty();
    }

    @Test
    void saveWithCompanyLink() {
        Officer officer = buildOfficer("bob@example.com");
        UUID companyId = UUID.randomUUID();
        officer.linkToCompany(CompanyLink.create(companyId, "Director", LocalDate.of(2024, 1, 1)));

        repository.save(officer);
        entityManager.flush();
        entityManager.clear();

        var found = repository.findById(officer.id());

        assertThat(found).isPresent();
        assertThat(found.get().companyLinks()).hasSize(1);
        assertThat(found.get().companyLinks().get(0).companyId()).isEqualTo(companyId);
        assertThat(found.get().companyLinks().get(0).title()).isEqualTo("Director");
        assertThat(found.get().companyLinks().get(0).active()).isTrue();
    }

    @Test
    void findByNameAndDateOfBirth() {
        Officer alice = buildOfficer("alice1@example.com");
        Officer alsoAlice = buildOfficer("alice2@example.com");
        Officer bob = Officer.create(
                "Bob", "Smith", LocalDate.of(1990, 1, 15),
                "British", new Address("2 St", "London", "EC1A", "UK"),
                "bob@example.com", null
        ).officer();

        repository.save(alice);
        repository.save(alsoAlice);
        repository.save(bob);

        List<Officer> results = repository.findByNameAndDateOfBirth("Alice", "Smith", LocalDate.of(1990, 1, 15));

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(o -> o.firstName().equals("Alice"));
    }

    @Test
    void deleteRemovesOfficer() {
        Officer officer = buildOfficer("delete@example.com");
        repository.save(officer);

        repository.delete(officer.id());

        assertThat(repository.findById(officer.id())).isEmpty();
    }

    @Test
    void updateOfficerPreservesLinks() {
        Officer officer = buildOfficer("update@example.com");
        UUID companyId = UUID.randomUUID();
        officer.linkToCompany(CompanyLink.create(companyId, "Director", LocalDate.of(2024, 1, 1)));
        repository.save(officer);
        entityManager.flush();
        entityManager.clear();

        Officer loaded = repository.findById(officer.id()).orElseThrow();
        loaded.update("Alicia", "Smith", "Spanish",
                new Address("2 Calle", "Madrid", "28001", "Spain"),
                "alicia@example.com", null);
        repository.save(loaded);
        entityManager.flush();
        entityManager.clear();

        var found = repository.findById(officer.id());
        assertThat(found).isPresent();
        assertThat(found.get().firstName()).isEqualTo("Alicia");
        assertThat(found.get().companyLinks()).hasSize(1);
    }
}
