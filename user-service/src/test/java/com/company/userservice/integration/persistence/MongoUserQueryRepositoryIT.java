package com.company.userservice.integration.persistence;

import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.infrastructure.adapter.out.persistence.query.MongoUserQueryRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import(MongoUserQueryRepository.class)
@Testcontainers
class MongoUserQueryRepositoryIT {

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
    MongoUserQueryRepository repository;

    @BeforeEach
    void reset() {
        repository.findAll().forEach(u -> repository.deleteById(u.id()));
    }

    private static UserReadModel model(String email, String first, String last) {
        return new UserReadModel(
                UserId.generate(),
                EmailAddress.of(email),
                first,
                last,
                Role.USER,
                true,
                Instant.now(),
                Instant.now());
    }

    @Test
    void saveAndFindById() {
        UserReadModel m = model("a@co.com", "A", "L");
        repository.save(m);

        assertThat(repository.findById(m.id())).isPresent();
        assertThat(repository.findById(m.id()).get().email().value()).isEqualTo("a@co.com");
    }

    @Test
    void findAllReturnsEveryone() {
        repository.save(model("a@co.com", "A", "L"));
        repository.save(model("b@co.com", "B", "L"));

        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void searchByEmail() {
        repository.save(model("alice@co.com", "Alice", "Smith"));
        repository.save(model("bob@co.com", "Bob", "Jones"));

        List<UserReadModel> result = repository.search("alice");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).firstName()).isEqualTo("Alice");
    }

    @Test
    void searchByLastName() {
        repository.save(model("a@co.com", "Alice", "Smith"));
        repository.save(model("b@co.com", "Bob", "Jones"));

        assertThat(repository.search("smith")).hasSize(1);
    }

    @Test
    void emptySearchReturnsAll() {
        repository.save(model("a@co.com", "A", "L"));

        assertThat(repository.search("")).hasSize(1);
    }

    @Test
    void deleteRemovesDocument() {
        UserReadModel m = model("a@co.com", "A", "L");
        repository.save(m);

        repository.deleteById(m.id());

        assertThat(repository.findById(m.id())).isEmpty();
    }
}
