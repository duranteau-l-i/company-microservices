package com.company.companyservice.integration.messaging;

import com.company.companyservice.config.KafkaConfig;
import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyStatus;
import com.company.companyservice.domain.model.OfficerSummary;
import com.company.companyservice.infrastructure.persistence.query.CompanyDocumentMapper;
import com.company.companyservice.infrastructure.persistence.query.CompanyMongoRepository;
import com.company.companyservice.infrastructure.persistence.query.MongoCompanyQueryRepository;
import com.company.companyservice.infrastructure.persistence.query.ProcessedEventDocument;
import com.company.companyservice.infrastructure.persistence.query.ProcessedEventMongoRepository;
import com.company.companyservice.presentation.consumer.CompanyEventConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DataMongoTest
@Import({
        KafkaConfig.class,
        CompanyEventConsumer.class,
        MongoCompanyQueryRepository.class
})
@EmbeddedKafka(
        partitions = 1,
        topics = {"company-events-consumer-it", "officer-events-consumer-it"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"}
)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.data.mongodb.auto-index-creation=true",
        "app.kafka.topics.company-events=company-events-consumer-it",
        "app.kafka.topics.officer-events=officer-events-consumer-it",
        "spring.application.name=company-service",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@Testcontainers
class CompanyEventConsumerIT {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    CompanyMongoRepository companyMongoRepository;

    @Autowired
    ProcessedEventMongoRepository processedEventMongoRepository;

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    /** A plain String/String producer so that messages are not double-serialized by JsonSerializer. */
    private KafkaTemplate<String, String> testProducer;

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        companyMongoRepository.deleteAll();
        processedEventMongoRepository.deleteAll();

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        testProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Test
    void companyCreatedEvent_createsMongoDocument() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();

        String envelope = buildEnvelope(eventId, "CompanyCreatedEvent", companyId,
                buildCreatedPayload(companyId, ownerId, now));

        testProducer.send("company-events-consumer-it", companyId.toString(), envelope);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<CompanyFullView> found = findById(companyId);
            assertThat(found).isPresent();
            assertThat(found.get().name()).isEqualTo("Acme Corp");
            assertThat(found.get().registrationNumber()).isEqualTo("REG-001");
            assertThat(found.get().ownerId()).isEqualTo(ownerId);
        });
    }

    @Test
    void companyUpdatedEvent_updatesMongoDocument() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant now = Instant.now();

        CompanyFullView existing = new CompanyFullView(
                CompanyId.of(companyId), "Original Corp", "REG-001",
                new Address("1 Main St", "Paris", "75001", "France"),
                ownerId, "John Doe", CompanyStatus.ACTIVE,
                now, now, List.of());
        companyMongoRepository.save(CompanyDocumentMapper.toDocument(existing));

        UUID eventId = UUID.randomUUID();
        String envelope = buildEnvelope(eventId, "CompanyUpdatedEvent", companyId,
                buildUpdatedPayload(companyId, now));

        testProducer.send("company-events-consumer-it", companyId.toString(), envelope);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<CompanyFullView> found = findById(companyId);
            assertThat(found).isPresent();
            assertThat(found.get().name()).isEqualTo("Updated Corp");
            assertThat(found.get().registrationNumber()).isEqualTo("REG-002");
            assertThat(found.get().ownerDisplayName()).isEqualTo("John Doe");
        });
    }

    @Test
    void companyUpdatedEvent_preservesOfficers() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID officerId = UUID.randomUUID();
        Instant now = Instant.now();

        OfficerSummary officer = new OfficerSummary(officerId, "Alice", "Smith", "CEO");
        CompanyFullView existing = new CompanyFullView(
                CompanyId.of(companyId), "Original Corp", "REG-001",
                new Address("1 Main St", "Paris", "75001", "France"),
                ownerId, "John Doe", CompanyStatus.ACTIVE,
                now, now, List.of(officer));
        companyMongoRepository.save(CompanyDocumentMapper.toDocument(existing));

        UUID eventId = UUID.randomUUID();
        String envelope = buildEnvelope(eventId, "CompanyUpdatedEvent", companyId,
                buildUpdatedPayload(companyId, now));

        testProducer.send("company-events-consumer-it", companyId.toString(), envelope);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<CompanyFullView> found = findById(companyId);
            assertThat(found).isPresent();
            assertThat(found.get().name()).isEqualTo("Updated Corp");
            assertThat(found.get().officers()).hasSize(1);
            assertThat(found.get().officers().get(0).officerId()).isEqualTo(officerId);
        });
    }

    @Test
    void companyDeletedEvent_removesMongoDocument() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant now = Instant.now();

        CompanyFullView existing = new CompanyFullView(
                CompanyId.of(companyId), "To Delete Corp", "REG-DEL",
                new Address("1 Main St", "Paris", "75001", "France"),
                ownerId, "Jane Doe", CompanyStatus.ACTIVE,
                now, now, List.of());
        companyMongoRepository.save(CompanyDocumentMapper.toDocument(existing));

        UUID eventId = UUID.randomUUID();
        String envelope = buildEnvelope(eventId, "CompanyDeletedEvent", companyId,
                buildDeletedPayload(companyId, ownerId, now));

        testProducer.send("company-events-consumer-it", companyId.toString(), envelope);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(findById(companyId)).isEmpty());
    }

    @Test
    void idempotency_sameEventPublishedTwice_processedOnlyOnce() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();

        String envelope = buildEnvelope(eventId, "CompanyCreatedEvent", companyId,
                buildCreatedPayload(companyId, ownerId, now));

        testProducer.send("company-events-consumer-it", companyId.toString(), envelope);
        testProducer.send("company-events-consumer-it", companyId.toString(), envelope);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(processedEventMongoRepository.existsById(eventId)).isTrue());

        // Wait briefly then assert no second entry was written for the duplicate event
        await().during(2, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<ProcessedEventDocument> entries = processedEventMongoRepository.findAll().stream()
                    .filter(e -> e.getEventId().equals(eventId))
                    .toList();
            assertThat(entries).hasSize(1);
        });
    }

    // ---- helpers ----

    private Optional<CompanyFullView> findById(UUID id) {
        return companyMongoRepository.findById(id)
                .map(CompanyDocumentMapper::toFullView);
    }

    private String buildEnvelope(UUID eventId, String eventType, UUID aggregateId, Object payload) throws Exception {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("eventId", eventId.toString());
        envelope.put("eventType", eventType);
        envelope.put("aggregateId", aggregateId.toString());
        envelope.put("aggregateType", "Company");
        envelope.put("timestamp", Instant.now().toString());
        envelope.put("version", 1);
        envelope.put("payload", payload);
        return MAPPER.writeValueAsString(envelope);
    }

    private Map<String, Object> buildCreatedPayload(UUID companyId, UUID ownerId, Instant now) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("aggregateId", companyId.toString());
        payload.put("name", "Acme Corp");
        payload.put("registrationNumber", "REG-001");
        payload.put("ownerId", ownerId.toString());
        payload.put("timestamp", now.toString());
        payload.put("version", 1);
        return payload;
    }

    private Map<String, Object> buildUpdatedPayload(UUID companyId, Instant now) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("aggregateId", companyId.toString());
        payload.put("name", "Updated Corp");
        payload.put("registrationNumber", "REG-002");
        payload.put("timestamp", now.toString());
        payload.put("version", 1);
        return payload;
    }

    private Map<String, Object> buildDeletedPayload(UUID companyId, UUID ownerId, Instant now) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("aggregateId", companyId.toString());
        payload.put("ownerId", ownerId.toString());
        payload.put("timestamp", now.toString());
        payload.put("version", 1);
        return payload;
    }
}
