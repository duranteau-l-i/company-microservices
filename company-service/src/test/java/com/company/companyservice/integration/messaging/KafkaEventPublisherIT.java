package com.company.companyservice.integration.messaging;

import com.company.companyservice.config.KafkaConfig;
import com.company.companyservice.domain.event.CompanyCreatedEvent;
import com.company.companyservice.domain.event.CompanyDeletedEvent;
import com.company.companyservice.domain.event.CompanyUpdatedEvent;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.infrastructure.messaging.KafkaEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {KafkaConfig.class, KafkaEventPublisher.class})
@Import(KafkaConfig.class)
@Testcontainers
class KafkaEventPublisherIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        r.add("spring.cloud.config.enabled", () -> "false");
        r.add("spring.cloud.discovery.enabled", () -> "false");
        r.add("eureka.client.enabled", () -> "false");
        r.add("app.kafka.topics.company-events", () -> "company-events");
    }

    @Autowired
    KafkaEventPublisher publisher;

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void publishCompanyCreatedEvent_serializesEnvelopeCorrectly() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        CompanyCreatedEvent event = CompanyCreatedEvent.of(
                CompanyId.of(companyId), "Acme Corp", "REG-001", ownerId, Instant.now());

        try (KafkaConsumer<String, String> consumer = newConsumer("company-events")) {
            publisher.publish(event);

            // Poll multiple times and look for our specific event by key
            ConsumerRecord<String, String> record = pollForKey(consumer, companyId.toString());
            assertThat(record).isNotNull();

            JsonNode envelope = MAPPER.readTree(record.value());
            assertThat(envelope.get("eventType").asText()).isEqualTo("CompanyCreatedEvent");
            assertThat(envelope.get("aggregateType").asText()).isEqualTo("Company");
            assertThat(envelope.get("aggregateId").asText()).isEqualTo(companyId.toString());
            assertThat(envelope.get("version").asInt()).isEqualTo(1);

            JsonNode payload = envelope.get("payload");
            assertThat(payload.get("name").asText()).isEqualTo("Acme Corp");
            assertThat(payload.get("registrationNumber").asText()).isEqualTo("REG-001");
            assertThat(payload.get("ownerId").asText()).isEqualTo(ownerId.toString());
        }
    }

    @Test
    void publishCompanyUpdatedEvent_serializesEnvelopeCorrectly() throws Exception {
        UUID companyId = UUID.randomUUID();
        CompanyUpdatedEvent event = CompanyUpdatedEvent.of(
                CompanyId.of(companyId), "Updated Corp", "REG-002", Instant.now());

        try (KafkaConsumer<String, String> consumer = newConsumer("company-events")) {
            publisher.publish(event);

            ConsumerRecord<String, String> record = pollForKey(consumer, companyId.toString());
            assertThat(record).isNotNull();

            JsonNode envelope = MAPPER.readTree(record.value());
            assertThat(envelope.get("eventType").asText()).isEqualTo("CompanyUpdatedEvent");
            assertThat(envelope.get("aggregateId").asText()).isEqualTo(companyId.toString());

            JsonNode payload = envelope.get("payload");
            assertThat(payload.get("name").asText()).isEqualTo("Updated Corp");
            assertThat(payload.get("registrationNumber").asText()).isEqualTo("REG-002");
        }
    }

    @Test
    void publishCompanyDeletedEvent_serializesEnvelopeCorrectly() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        CompanyDeletedEvent event = CompanyDeletedEvent.of(
                CompanyId.of(companyId), ownerId, Instant.now());

        try (KafkaConsumer<String, String> consumer = newConsumer("company-events")) {
            publisher.publish(event);

            ConsumerRecord<String, String> record = pollForKey(consumer, companyId.toString());
            assertThat(record).isNotNull();

            JsonNode envelope = MAPPER.readTree(record.value());
            assertThat(envelope.get("eventType").asText()).isEqualTo("CompanyDeletedEvent");
            assertThat(envelope.get("aggregateId").asText()).isEqualTo(companyId.toString());
        }
    }

    /**
     * Polls for a record with the given key, retrying up to 10 seconds total.
     * This handles the case where multiple tests share the same topic and earlier
     * test messages are replayed from the beginning with auto-offset-reset=earliest.
     */
    private ConsumerRecord<String, String> pollForKey(KafkaConsumer<String, String> consumer, String key) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                if (key.equals(record.key())) {
                    return record;
                }
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
