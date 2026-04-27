package com.company.officerservice.integration.persistence;

import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.model.OfficerRestrictedView;
import com.company.officerservice.infrastructure.persistence.query.OfficerDocumentRepository;
import com.company.officerservice.infrastructure.persistence.query.OfficerQueryRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import(OfficerQueryRepositoryAdapter.class)
@Testcontainers
class OfficerQueryRepositoryIT {

    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
        r.add("spring.cloud.config.enabled", () -> "false");
        r.add("spring.cloud.discovery.enabled", () -> "false");
        r.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    OfficerQueryRepositoryAdapter repository;

    @Autowired
    OfficerDocumentRepository documentRepository;

    @BeforeEach
    void reset() {
        documentRepository.deleteAll();
    }

    private OfficerFullView buildView(String firstName, String lastName, LocalDate dob) {
        return new OfficerFullView(
                OfficerId.generate(), firstName, lastName, dob,
                "French", new Address("1 Rue", "Paris", "75001", "France"),
                firstName.toLowerCase() + "@example.com", null,
                List.of(), Instant.now(), Instant.now()
        );
    }

    @Test
    void saveAndFindFullById() {
        OfficerFullView view = buildView("Alice", "Smith", LocalDate.of(1990, 1, 15));
        repository.save(view);

        Optional<OfficerFullView> found = repository.findFullById(view.id());

        assertThat(found).isPresent();
        assertThat(found.get().firstName()).isEqualTo("Alice");
        assertThat(found.get().email()).isEqualTo("alice@example.com");
    }

    @Test
    void findRestrictedById() {
        OfficerFullView view = buildView("Bob", "Jones", LocalDate.of(1985, 5, 20));
        repository.save(view);

        Optional<OfficerRestrictedView> found = repository.findRestrictedById(view.id());

        assertThat(found).isPresent();
        assertThat(found.get().firstName()).isEqualTo("Bob");
        assertThat(found.get().lastName()).isEqualTo("Jones");
    }

    @Test
    void findByCompanyId() {
        UUID companyId = UUID.randomUUID();
        CompanyLink activeLink = CompanyLink.create(companyId, "Director", LocalDate.of(2024, 1, 1));
        CompanyLink otherLink = CompanyLink.create(UUID.randomUUID(), "CEO", LocalDate.of(2024, 1, 1));

        OfficerFullView withLink = new OfficerFullView(
                OfficerId.generate(), "Alice", "Smith", LocalDate.of(1990, 1, 15),
                "French", new Address("1 Rue", "Paris", "75001", "France"),
                "alice@example.com", null,
                List.of(activeLink), Instant.now(), Instant.now()
        );
        OfficerFullView withOtherLink = new OfficerFullView(
                OfficerId.generate(), "Bob", "Jones", LocalDate.of(1985, 5, 20),
                "British", new Address("2 St", "London", "EC1A", "UK"),
                "bob@example.com", null,
                List.of(otherLink), Instant.now(), Instant.now()
        );

        repository.save(withLink);
        repository.save(withOtherLink);

        List<OfficerFullView> results = repository.findByCompanyId(companyId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).firstName()).isEqualTo("Alice");
    }

    @Test
    void searchByFirstName() {
        repository.save(buildView("Alice", "Smith", LocalDate.of(1990, 1, 15)));
        repository.save(buildView("Alice", "Johnson", LocalDate.of(1985, 5, 20)));
        repository.save(buildView("Bob", "Smith", LocalDate.of(1990, 1, 15)));

        List<OfficerRestrictedView> results = repository.search("alice", null, null);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(v -> v.firstName().equalsIgnoreCase("Alice"));
    }

    @Test
    void searchByLastName() {
        repository.save(buildView("Alice", "Smith", LocalDate.of(1990, 1, 15)));
        repository.save(buildView("Bob", "Smith", LocalDate.of(1985, 5, 20)));
        repository.save(buildView("Carol", "Jones", LocalDate.of(1992, 3, 10)));

        List<OfficerRestrictedView> results = repository.search(null, "Smith", null);

        assertThat(results).hasSize(2);
    }

    @Test
    void searchByFullNameAndDob() {
        repository.save(buildView("Alice", "Smith", LocalDate.of(1990, 1, 15)));
        repository.save(buildView("Alice", "Smith", LocalDate.of(1985, 5, 20)));

        List<OfficerRestrictedView> results = repository.search("Alice", "Smith", LocalDate.of(1990, 1, 15));

        assertThat(results).hasSize(1);
    }

    @Test
    void searchWithNoFiltersReturnsAll() {
        repository.save(buildView("Alice", "Smith", LocalDate.of(1990, 1, 15)));
        repository.save(buildView("Bob", "Jones", LocalDate.of(1985, 5, 20)));

        List<OfficerRestrictedView> results = repository.search(null, null, null);

        assertThat(results).hasSize(2);
    }

    @Test
    void deleteById() {
        OfficerFullView view = buildView("ToDelete", "Person", LocalDate.of(1990, 1, 1));
        repository.save(view);

        repository.deleteById(view.id());

        assertThat(repository.findFullById(view.id())).isEmpty();
    }
}
