package com.company.officerservice.integration.messaging;

import com.company.officerservice.config.KafkaConfig;
import com.company.officerservice.infrastructure.messaging.KafkaOutboxSender;
import com.company.officerservice.infrastructure.messaging.OutboxRelayScheduler;
import com.company.officerservice.infrastructure.messaging.OutboxRelayService;
import com.company.officerservice.infrastructure.persistence.command.JpaOutboxRelayRepository;
import com.company.officerservice.infrastructure.persistence.command.OutboxEventEntity;
import com.company.officerservice.infrastructure.persistence.command.OutboxEventEntityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({KafkaConfig.class, KafkaOutboxSender.class, JpaOutboxRelayRepository.class,
        OutboxRelayService.class, OutboxRelayScheduler.class})
@Testcontainers
class OutboxRelaySchedulerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

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
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        r.add("app.kafka.topics.officer-events", () -> "officer-events");
        r.add("app.outbox.relay-interval-ms", () -> "300000");
    }

    @Autowired
    OutboxEventEntityRepository outboxRepository;

    @Autowired
    OutboxRelayScheduler relayScheduler;

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @AfterEach
    void cleanup() {
        outboxRepository.deleteAll();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void relay_sendsPendingRowToKafkaAndMarksPublished() throws Exception {
        UUID aggregateId = UUID.randomUUID();
        String payload = "{\"eventId\":\"" + UUID.randomUUID() + "\",\"eventType\":\"OfficerCreatedEvent\","
                + "\"aggregateId\":\"" + aggregateId + "\",\"aggregateType\":\"Officer\","
                + "\"timestamp\":\"2026-01-01T00:00:00Z\",\"version\":1,\"payload\":{}}";

        OutboxEventEntity pending = new OutboxEventEntity(
                UUID.randomUUID(), aggregateId, "OfficerCreatedEvent",
                "officer-events", payload, "PENDING", Instant.now(), null);
        outboxRepository.save(pending);

        try (KafkaConsumer<String, String> consumer = newConsumer("officer-events")) {
            relayScheduler.relay();

            ConsumerRecord<String, String> record = pollForKey(consumer, aggregateId.toString());
            assertThat(record).isNotNull();

            JsonNode envelope = MAPPER.readTree(record.value());
            assertThat(envelope.get("eventType").asText()).isEqualTo("OfficerCreatedEvent");
            assertThat(envelope.get("aggregateId").asText()).isEqualTo(aggregateId.toString());
        }

        OutboxEventEntity updated = outboxRepository.findById(pending.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("PUBLISHED");
        assertThat(updated.getPublishedAt()).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void relay_skipsAlreadyPublishedRows() {
        UUID aggregateId = UUID.randomUUID();
        OutboxEventEntity published = new OutboxEventEntity(
                UUID.randomUUID(), aggregateId, "OfficerCreatedEvent",
                "officer-events", "{}", "PUBLISHED", Instant.now(), Instant.now());
        outboxRepository.save(published);

        relayScheduler.relay();

        OutboxEventEntity unchanged = outboxRepository.findById(published.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo("PUBLISHED");
    }

    private ConsumerRecord<String, String> pollForKey(KafkaConsumer<String, String> consumer, String key) {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                if (key.equals(record.key())) return record;
            }
        }
        return null;
    }

    private KafkaConsumer<String, String> newConsumer(String topic) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(topic));
        return consumer;
    }
}
