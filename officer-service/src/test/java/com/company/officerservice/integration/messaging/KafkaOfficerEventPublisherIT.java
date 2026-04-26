package com.company.officerservice.integration.messaging;

import com.company.officerservice.config.KafkaConfig;
import com.company.officerservice.domain.event.OfficerCreatedEvent;
import com.company.officerservice.domain.event.OfficerDeletedEvent;
import com.company.officerservice.domain.event.OfficerLinkedToCompanyEvent;
import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.infrastructure.messaging.KafkaOfficerEventPublisher;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {KafkaConfig.class, KafkaOfficerEventPublisher.class})
@Import(KafkaConfig.class)
@Testcontainers
class KafkaOfficerEventPublisherIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        r.add("spring.cloud.config.enabled", () -> "false");
        r.add("spring.cloud.discovery.enabled", () -> "false");
        r.add("eureka.client.enabled", () -> "false");
        r.add("app.kafka.topics.officer-events", () -> "officer-events");
    }

    @Autowired
    KafkaOfficerEventPublisher publisher;

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void publishOfficerCreatedEvent_serializesEnvelopeCorrectly() throws Exception {
        Officer officer = buildOfficer();
        OfficerCreatedEvent event = OfficerCreatedEvent.of(officer);

        try (KafkaConsumer<String, String> consumer = newConsumer("officer-events")) {
            publisher.publish(event);

            ConsumerRecord<String, String> record = pollForKey(consumer, officer.id().value().toString());
            assertThat(record).isNotNull();

            JsonNode envelope = MAPPER.readTree(record.value());
            assertThat(envelope.get("eventType").asText()).isEqualTo("OfficerCreatedEvent");
            assertThat(envelope.get("aggregateType").asText()).isEqualTo("Officer");
            assertThat(envelope.get("aggregateId").asText()).isEqualTo(officer.id().value().toString());
            assertThat(envelope.get("version").asInt()).isEqualTo(1);

            JsonNode payload = envelope.get("payload");
            assertThat(payload.get("firstName").asText()).isEqualTo("Alice");
            assertThat(payload.get("lastName").asText()).isEqualTo("Smith");
        }
    }

    @Test
    void publishOfficerDeletedEvent_serializesEnvelopeCorrectly() throws Exception {
        UUID officerId = UUID.randomUUID();
        OfficerDeletedEvent event = OfficerDeletedEvent.of(officerId, "Alice", "Smith");

        try (KafkaConsumer<String, String> consumer = newConsumer("officer-events")) {
            publisher.publish(event);

            ConsumerRecord<String, String> record = pollForKey(consumer, officerId.toString());
            assertThat(record).isNotNull();

            JsonNode envelope = MAPPER.readTree(record.value());
            assertThat(envelope.get("eventType").asText()).isEqualTo("OfficerDeletedEvent");
        }
    }

    @Test
    void publishOfficerLinkedEvent_serializesEnvelopeCorrectly() throws Exception {
        Officer officer = buildOfficer();
        UUID companyId = UUID.randomUUID();
        OfficerLinkedToCompanyEvent event = OfficerLinkedToCompanyEvent.of(officer, companyId, "Director");

        try (KafkaConsumer<String, String> consumer = newConsumer("officer-events")) {
            publisher.publish(event);

            ConsumerRecord<String, String> record = pollForKey(consumer, officer.id().value().toString());
            assertThat(record).isNotNull();

            JsonNode envelope = MAPPER.readTree(record.value());
            assertThat(envelope.get("eventType").asText()).isEqualTo("OfficerLinkedToCompanyEvent");

            JsonNode payload = envelope.get("payload");
            assertThat(payload.get("companyId").asText()).isEqualTo(companyId.toString());
            assertThat(payload.get("title").asText()).isEqualTo("Director");
        }
    }

    private static Officer buildOfficer() {
        return Officer.create(
                "Alice", "Smith", LocalDate.of(1990, 1, 15),
                "French", new Address("1 Rue", "Paris", "75001", "France"),
                "alice@example.com", null
        ).officer();
    }

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
