package com.company.officerservice.integration.messaging;

import com.company.officerservice.config.KafkaConfig;
import com.company.officerservice.domain.model.Address;
import com.company.officerservice.domain.model.CompanyLink;
import com.company.officerservice.domain.model.Officer;
import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.infrastructure.persistence.query.KnownCompanyDocument;
import com.company.officerservice.infrastructure.persistence.query.KnownCompanyRepository;
import com.company.officerservice.infrastructure.persistence.query.ProcessedEventRepository;
import com.company.officerservice.presentation.consumer.CompanyEventConsumer;
import com.company.officerservice.stubs.InMemoryOfficerCommandRepository;
import com.company.officerservice.stubs.InMemoryOfficerQueryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DataMongoTest
@Import({
        KafkaConfig.class,
        CompanyEventConsumer.class,
        CompanyEventConsumerIT.StubRepositoriesConfig.class
})
@EmbeddedKafka(
        partitions = 1,
        topics = {"company-events-company-consumer-it"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"}
)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.data.mongodb.auto-index-creation=true",
        "app.kafka.topics.company-events=company-events-company-consumer-it",
        "app.kafka.topics.officer-events=officer-events-company-consumer-it",
        "spring.application.name=officer-service",
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
    KnownCompanyRepository knownCompanyRepository;

    @Autowired
    ProcessedEventRepository processedEventRepository;

    @Autowired
    InMemoryOfficerCommandRepository commandRepository;

    @Autowired
    InMemoryOfficerQueryRepository queryRepository;

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    private KafkaTemplate<String, String> testProducer;

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        knownCompanyRepository.deleteAll();
        processedEventRepository.deleteAll();
        commandRepository.clear();
        queryRepository.clear();

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        testProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Test
    void companyCreatedEvent_addsCompanyToKnownCompanies() throws Exception {
        UUID companyId = UUID.randomUUID();

        String envelope = buildEnvelope(UUID.randomUUID(), "CompanyCreatedEvent", companyId,
                buildCreatedPayload(companyId));

        testProducer.send("company-events-company-consumer-it", companyId.toString(), envelope);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(knownCompanyRepository.existsById(companyId)).isTrue());
    }

    @Test
    void companyDeletedEvent_removesCompanyFromKnownCompanies() throws Exception {
        UUID companyId = UUID.randomUUID();
        knownCompanyRepository.save(new KnownCompanyDocument(companyId));

        String envelope = buildEnvelope(UUID.randomUUID(), "CompanyDeletedEvent", companyId,
                buildDeletedPayload(companyId));

        testProducer.send("company-events-company-consumer-it", companyId.toString(), envelope);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(knownCompanyRepository.existsById(companyId)).isFalse());
    }

    @Test
    void companyDeletedEvent_unlinksOfficersAndRemovesFromKnownCompanies() throws Exception {
        UUID companyId = UUID.randomUUID();

        // Seed a known company
        knownCompanyRepository.save(new KnownCompanyDocument(companyId));

        // Seed an officer linked to the company in both write and read stores
        Officer.Created created = Officer.create(
                "Alice", "Smith", LocalDate.of(1990, 1, 15),
                "French", new Address("1 Rue", "Paris", "75001", "France"),
                "alice@example.com", null
        );
        Officer officer = created.officer();
        officer.linkToCompany(CompanyLink.create(companyId, "Director", LocalDate.of(2024, 1, 1)));
        commandRepository.save(officer);

        OfficerFullView view = new OfficerFullView(
                officer.id(), officer.firstName(), officer.lastName(), officer.dateOfBirth(),
                officer.nationality(), officer.address(), officer.email(), officer.phone(),
                officer.companyLinks(), officer.createdAt(), officer.updatedAt()
        );
        queryRepository.save(view);

        // Send CompanyDeletedEvent
        String envelope = buildEnvelope(UUID.randomUUID(), "CompanyDeletedEvent", companyId,
                buildDeletedPayload(companyId));

        testProducer.send("company-events-company-consumer-it", companyId.toString(), envelope);

        // Assert: known_companies entry removed
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(knownCompanyRepository.existsById(companyId)).isFalse());

        // Assert: officer's company link is now inactive
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Officer updated = commandRepository.findById(officer.id()).orElseThrow();
            assertThat(updated.companyLinks()).hasSize(1);
            assertThat(updated.companyLinks().get(0).active()).isFalse();
        });
    }

    @Test
    void duplicateCompanyCreatedEvent_isIdempotent() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        String envelope = buildEnvelope(eventId, "CompanyCreatedEvent", companyId,
                buildCreatedPayload(companyId));

        testProducer.send("company-events-company-consumer-it", companyId.toString(), envelope);
        testProducer.send("company-events-company-consumer-it", companyId.toString(), envelope);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(processedEventRepository.existsById(eventId)).isTrue());

        // Wait a bit then verify only one entry exists in known_companies
        await().during(2, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(knownCompanyRepository.findAll()).hasSize(1));
    }

    // ---- helpers ----

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

    private Map<String, Object> buildCreatedPayload(UUID companyId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("aggregateId", companyId.toString());
        payload.put("name", "Acme Corp");
        payload.put("registrationNumber", "REG-001");
        payload.put("ownerId", UUID.randomUUID().toString());
        payload.put("timestamp", Instant.now().toString());
        payload.put("version", 1);
        return payload;
    }

    private Map<String, Object> buildDeletedPayload(UUID companyId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("aggregateId", companyId.toString());
        payload.put("ownerId", UUID.randomUUID().toString());
        payload.put("timestamp", Instant.now().toString());
        payload.put("version", 1);
        return payload;
    }

    @TestConfiguration
    static class StubRepositoriesConfig {

        @Bean
        InMemoryOfficerCommandRepository officerCommandRepository() {
            return new InMemoryOfficerCommandRepository();
        }

        @Bean
        InMemoryOfficerQueryRepository officerQueryRepository() {
            return new InMemoryOfficerQueryRepository();
        }
    }
}
