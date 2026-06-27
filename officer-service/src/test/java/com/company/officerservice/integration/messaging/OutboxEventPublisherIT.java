package com.company.officerservice.integration.messaging;

import com.company.officerservice.config.KafkaConfig;
import com.company.officerservice.domain.event.OfficerCreatedEvent;
import com.company.officerservice.infrastructure.messaging.OutboxOfficerEventPublisher;
import com.company.officerservice.infrastructure.persistence.command.OutboxEventEntity;
import com.company.officerservice.infrastructure.persistence.command.OutboxEventEntityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({KafkaConfig.class, OutboxOfficerEventPublisher.class})
@Testcontainers
class OutboxEventPublisherIT {

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
        r.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        r.add("app.kafka.topics.officer-events", () -> "officer-events");
    }

    @Autowired
    OutboxOfficerEventPublisher publisher;

    @Autowired
    OutboxEventEntityRepository outboxRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void publish_writesPendingRowWithCorrectFields() {
        UUID officerId = UUID.randomUUID();
        OfficerCreatedEvent event = new OfficerCreatedEvent(
                UUID.randomUUID(), officerId,
                "Alice", "Smith",
                LocalDate.of(1990, 1, 1), "French",
                "alice@example.com", "+33600000000",
                Instant.now(), 1);

        publisher.publish(event);

        List<OutboxEventEntity> rows = outboxRepository.findPending(10);
        assertThat(rows).hasSize(1);
        OutboxEventEntity row = rows.get(0);
        assertThat(row.getEventType()).isEqualTo("OfficerCreatedEvent");
        assertThat(row.getTopic()).isEqualTo("officer-events");
        assertThat(row.getStatus()).isEqualTo("PENDING");
        assertThat(row.getAggregateId()).isEqualTo(officerId);
        assertThat(row.getPayload()).contains("OfficerCreatedEvent");
        assertThat(row.getPayload()).contains("alice@example.com");
        assertThat(row.getPublishedAt()).isNull();
    }

    @Test
    void publish_withinRolledBackTransaction_leavesNoRow() {
        UUID officerId = UUID.randomUUID();
        OfficerCreatedEvent event = new OfficerCreatedEvent(
                UUID.randomUUID(), officerId,
                "Bob", "Rollback",
                LocalDate.of(1985, 6, 15), "British",
                "bob@example.com", "+44700000000",
                Instant.now(), 1);

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.execute(status -> {
            publisher.publish(event);
            status.setRollbackOnly();
            return null;
        });

        assertThat(outboxRepository.findPending(10)
                .stream().filter(r -> r.getAggregateId().equals(officerId)).toList())
                .isEmpty();
    }
}
