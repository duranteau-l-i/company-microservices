package com.company.userservice.integration.consumer;

import com.company.userservice.domain.event.UserCreatedEvent;
import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.infrastructure.messaging.KafkaUserEventPublisher;
import com.company.userservice.config.KafkaConfig;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {KafkaConfig.class, KafkaUserEventPublisher.class})
@Import(KafkaConfig.class)
@Testcontainers
class KafkaUserEventPublisherIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        r.add("spring.cloud.config.enabled", () -> "false");
        r.add("spring.cloud.discovery.enabled", () -> "false");
        r.add("eureka.client.enabled", () -> "false");
        r.add("app.kafka.topics.user-events", () -> "user-events");
    }

    @Autowired
    KafkaUserEventPublisher publisher;

    @Test
    void publishSerializesEnvelopeAndRoutes() throws Exception {
        UUID userId = UUID.randomUUID();
        UserCreatedEvent event = UserCreatedEvent.of(
                UserId.of(userId),
                EmailAddress.of("user@test.com"),
                Role.USER,
                Instant.now());

        try (KafkaConsumer<String, String> consumer = newConsumer()) {
            consumer.subscribe(List.of("user-events"));

            publisher.publish(event);

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.count()).isEqualTo(1);

            ConsumerRecord<String, String> record = records.iterator().next();
            assertThat(record.key()).isEqualTo(userId.toString());

            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            JsonNode envelope = mapper.readTree(record.value());

            assertThat(envelope.get("eventType").asText()).isEqualTo("UserCreatedEvent");
            assertThat(envelope.get("aggregateType").asText()).isEqualTo("User");
            assertThat(envelope.get("aggregateId").asText()).isEqualTo(userId.toString());
            assertThat(envelope.get("payload").get("email").asText()).isEqualTo("user@test.com");
        }
    }

    private KafkaConsumer<String, String> newConsumer() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new KafkaConsumer<>(props);
    }
}
