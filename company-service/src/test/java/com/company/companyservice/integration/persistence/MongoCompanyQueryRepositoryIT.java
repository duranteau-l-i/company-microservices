package com.company.companyservice.integration.persistence;

import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.model.CompanyStatus;
import com.company.companyservice.domain.model.OfficerSummary;
import com.company.companyservice.infrastructure.persistence.query.MongoCompanyQueryRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import(MongoCompanyQueryRepository.class)
@Testcontainers
class MongoCompanyQueryRepositoryIT {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        r.add("spring.cloud.config.enabled", () -> "false");
        r.add("spring.cloud.discovery.enabled", () -> "false");
        r.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    MongoCompanyQueryRepository repository;

    @BeforeEach
    void reset() {
        repository.findAllFull().forEach(v -> repository.deleteById(v.id()));
    }

    private CompanyFullView buildView(String name, UUID ownerId) {
        return new CompanyFullView(
                CompanyId.generate(),
                name,
                "REG-" + UUID.randomUUID().toString().substring(0, 8),
                new Address("1 Main St", "Paris", "75001", "France"),
                ownerId,
                "John Doe",
                CompanyStatus.ACTIVE,
                Instant.now(),
                Instant.now(),
                List.of()
        );
    }

    @Test
    void saveAndFindFullById() {
        UUID ownerId = UUID.randomUUID();
        OfficerSummary officer = new OfficerSummary(UUID.randomUUID(), "Alice", "Smith", "CEO");
        CompanyFullView view = new CompanyFullView(
                CompanyId.generate(),
                "Acme Corp",
                "REG-001",
                new Address("1 Main St", "Paris", "75001", "France"),
                ownerId,
                "John Doe",
                CompanyStatus.ACTIVE,
                Instant.now(),
                Instant.now(),
                List.of(officer)
        );

        repository.save(view);

        Optional<CompanyFullView> found = repository.findFullById(view.id());
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Acme Corp");
        assertThat(found.get().ownerId()).isEqualTo(ownerId);
        assertThat(found.get().officers()).hasSize(1);
        assertThat(found.get().officers().get(0).firstName()).isEqualTo("Alice");
    }

    @Test
    void findRestrictedById() {
        UUID ownerId = UUID.randomUUID();
        CompanyFullView view = buildView("Beta Ltd", ownerId);

        repository.save(view);

        Optional<CompanyRestrictedView> found = repository.findRestrictedById(view.id());
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Beta Ltd");
        assertThat(found.get().ownerId()).isEqualTo(ownerId);
        assertThat(found.get().status()).isEqualTo(CompanyStatus.ACTIVE);
    }

    @Test
    void findFullByOwnerId() {
        UUID owner1 = UUID.randomUUID();
        UUID owner2 = UUID.randomUUID();
        repository.save(buildView("Owner1 Corp", owner1));
        repository.save(buildView("Owner2 Corp", owner2));

        List<CompanyFullView> result = repository.findFullByOwnerId(owner1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Owner1 Corp");
    }

    @Test
    void findAllFull() {
        repository.save(buildView("Alpha Inc", UUID.randomUUID()));
        repository.save(buildView("Gamma LLC", UUID.randomUUID()));

        List<CompanyFullView> all = repository.findAllFull();

        assertThat(all).hasSize(2);
    }

    @Test
    void searchByName() {
        repository.save(buildView("Acme Corp", UUID.randomUUID()));
        repository.save(buildView("Other Company", UUID.randomUUID()));

        List<CompanyRestrictedView> result = repository.search("acme");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Acme Corp");
    }

    @Test
    void searchNoMatch() {
        repository.save(buildView("Acme Corp", UUID.randomUUID()));

        List<CompanyRestrictedView> result = repository.search("zzz");

        assertThat(result).isEmpty();
    }

    @Test
    void deleteById() {
        CompanyFullView view = buildView("ToDelete Inc", UUID.randomUUID());
        repository.save(view);

        repository.deleteById(view.id());

        assertThat(repository.findFullById(view.id())).isEmpty();
    }
}
