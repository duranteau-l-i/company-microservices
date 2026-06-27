package com.company.companyservice.integration.messaging;

import com.company.companyservice.config.KafkaConfig;
import com.company.companyservice.domain.event.CompanyCreatedEvent;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.infrastructure.messaging.OutboxCompanyEventPublisher;
import com.company.companyservice.infrastructure.persistence.command.OutboxEventEntity;
import com.company.companyservice.infrastructure.persistence.command.OutboxEventEntityRepository;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({KafkaConfig.class, OutboxCompanyEventPublisher.class})
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
        r.add("app.kafka.topics.company-events", () -> "company-events");
    }

    @Autowired
    OutboxCompanyEventPublisher publisher;

    @Autowired
    OutboxEventEntityRepository outboxRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void publish_writesPendingRowWithCorrectFields() {
        UUID companyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        CompanyCreatedEvent event = CompanyCreatedEvent.of(
                CompanyId.of(companyId), "Acme Corp", "REG-001", ownerId, Instant.now());

        publisher.publish(event);

        List<OutboxEventEntity> rows = outboxRepository.findPending(10);
        assertThat(rows).hasSize(1);
        OutboxEventEntity row = rows.get(0);
        assertThat(row.getEventType()).isEqualTo("CompanyCreatedEvent");
        assertThat(row.getTopic()).isEqualTo("company-events");
        assertThat(row.getStatus()).isEqualTo("PENDING");
        assertThat(row.getAggregateId()).isEqualTo(companyId);
        assertThat(row.getPayload()).contains("CompanyCreatedEvent");
        assertThat(row.getPayload()).contains("Acme Corp");
        assertThat(row.getPublishedAt()).isNull();
    }

    @Test
    void publish_withinRolledBackTransaction_leavesNoRow() {
        UUID companyId = UUID.randomUUID();
        CompanyCreatedEvent event = CompanyCreatedEvent.of(
                CompanyId.of(companyId), "Rollback Corp", "REG-ROLL", UUID.randomUUID(), Instant.now());

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.execute(status -> {
            publisher.publish(event);
            status.setRollbackOnly();
            return null;
        });

        assertThat(outboxRepository.findPending(10)
                .stream().filter(r -> r.getAggregateId().equals(companyId)).toList())
                .isEmpty();
    }
}
