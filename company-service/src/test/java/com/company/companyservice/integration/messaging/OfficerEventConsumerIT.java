package com.company.companyservice.integration.messaging;

import com.company.companyservice.config.KafkaConfig;
import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyStatus;
import com.company.companyservice.domain.model.OfficerSummary;
import com.company.companyservice.infrastructure.persistence.query.CompanyDocumentMapper;
import com.company.companyservice.infrastructure.persistence.query.CompanyDocumentRepository;
import com.company.companyservice.infrastructure.persistence.query.CompanyQueryRepositoryAdapter;
import com.company.companyservice.infrastructure.persistence.query.ProcessedEventDocumentRepository;
import com.company.companyservice.presentation.consumer.OfficerEventConsumer;
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
        OfficerEventConsumer.class,
        CompanyQueryRepositoryAdapter.class
})
@EmbeddedKafka(
        partitions = 1,
        topics = {"officer-events-officer-it"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"}
)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.data.mongodb.auto-index-creation=true",
        "app.kafka.topics.company-events=company-events-officer-it",
        "app.kafka.topics.officer-events=officer-events-officer-it",
        "spring.application.name=company-service",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@Testcontainers
class OfficerEventConsumerIT {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    CompanyDocumentRepository companyDocumentRepository;

    @Autowired
    ProcessedEventDocumentRepository processedEventDocumentRepository;

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    private KafkaTemplate<String, String> testProducer;

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        companyDocumentRepository.deleteAll();
        processedEventDocumentRepository.deleteAll();

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        testProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Test
    void officerLinkedEvent_addsOfficerToCompany() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID officerId = UUID.randomUUID();
        Instant now = Instant.now();

        seedCompany(companyId, "Acme Corp", List.of());

        String envelope = buildEnvelope(UUID.randomUUID(), "OfficerLinkedToCompanyEvent", officerId,
                buildLinkedPayload(officerId, companyId, "Alice", "Smith", "CEO"));

        testProducer.send("officer-events-officer-it", companyId.toString(), envelope);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<CompanyFullView> found = findById(companyId);
            assertThat(found).isPresent();
            assertThat(found.get().officers()).hasSize(1);
            assertThat(found.get().officers().get(0).officerId()).isEqualTo(officerId);
            assertThat(found.get().officers().get(0).firstName()).isEqualTo("Alice");
        });
    }

    @Test
    void officerUnlinkedEvent_removesOfficerFromCompany() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID officerId = UUID.randomUUID();
        Instant now = Instant.now();

        OfficerSummary officer = new OfficerSummary(officerId, "Bob", "Jones", "CFO");
        seedCompany(companyId, "Beta Ltd", List.of(officer));

        String envelope = buildEnvelope(UUID.randomUUID(), "OfficerUnlinkedFromCompanyEvent", officerId,
                buildUnlinkedPayload(officerId, companyId));

        testProducer.send("officer-events-officer-it", companyId.toString(), envelope);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<CompanyFullView> found = findById(companyId);
            assertThat(found).isPresent();
            assertThat(found.get().officers()).isEmpty();
        });
    }

    @Test
    void officerUpdatedEvent_updatesOfficerNameInLinkedCompany() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID officerId = UUID.randomUUID();

        OfficerSummary officer = new OfficerSummary(officerId, "Alice", "Smith", "CEO");
        seedCompany(companyId, "Acme Corp", List.of(officer));

        String envelope = buildEnvelope(UUID.randomUUID(), "OfficerUpdatedEvent", officerId,
                buildUpdatedPayload(officerId, "Alice", "Johnson"));

        testProducer.send("officer-events-officer-it", officerId.toString(), envelope);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<CompanyFullView> found = findById(companyId);
            assertThat(found).isPresent();
            assertThat(found.get().officers()).hasSize(1);
            assertThat(found.get().officers().get(0).lastName()).isEqualTo("Johnson");
        });
    }

    @Test
    void officerDeletedEvent_removesOfficerFromLinkedCompany() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID officerId = UUID.randomUUID();

        OfficerSummary officer = new OfficerSummary(officerId, "Bob", "Jones", "CFO");
        seedCompany(companyId, "Beta Ltd", List.of(officer));

        String envelope = buildEnvelope(UUID.randomUUID(), "OfficerDeletedEvent", officerId,
                buildDeletedPayload(officerId));

        testProducer.send("officer-events-officer-it", officerId.toString(), envelope);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<CompanyFullView> found = findById(companyId);
            assertThat(found).isPresent();
            assertThat(found.get().officers()).isEmpty();
        });
    }

    @Test
    void officerLinkedEvent_duplicateDelivery_officerNotAddedTwice() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID officerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        seedCompany(companyId, "Gamma Inc", List.of());

        String envelope = buildEnvelope(eventId, "OfficerLinkedToCompanyEvent", officerId,
                buildLinkedPayload(officerId, companyId, "Carol", "White", "CTO"));

        testProducer.send("officer-events-officer-it", companyId.toString(), envelope);
        testProducer.send("officer-events-officer-it", companyId.toString(), envelope);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(processedEventDocumentRepository.existsById(eventId)).isTrue());

        await().during(2, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<CompanyFullView> found = findById(companyId);
            assertThat(found).isPresent();
            assertThat(found.get().officers()).hasSize(1);
        });
    }

    // ---- helpers ----

    private void seedCompany(UUID companyId, String name, List<OfficerSummary> officers) {
        Instant now = Instant.now();
        CompanyFullView view = new CompanyFullView(
                CompanyId.of(companyId), name, "REG-" + companyId.toString().substring(0, 8),
                new Address("1 Main St", "Paris", "75001", "France"),
                UUID.randomUUID(), "Owner Name", CompanyStatus.ACTIVE,
                now, now, officers);
        companyDocumentRepository.save(CompanyDocumentMapper.toDocument(view));
    }

    private Optional<CompanyFullView> findById(UUID id) {
        return companyDocumentRepository.findById(id).map(CompanyDocumentMapper::toFullView);
    }

    private String buildEnvelope(UUID eventId, String eventType, UUID aggregateId, Object payload) throws Exception {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("eventId", eventId.toString());
        envelope.put("eventType", eventType);
        envelope.put("aggregateId", aggregateId.toString());
        envelope.put("aggregateType", "Officer");
        envelope.put("timestamp", Instant.now().toString());
        envelope.put("version", 1);
        envelope.put("payload", payload);
        return MAPPER.writeValueAsString(envelope);
    }

    private Map<String, Object> buildLinkedPayload(UUID officerId, UUID companyId,
                                                    String firstName, String lastName, String title) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("aggregateId", officerId.toString());
        payload.put("companyId", companyId.toString());
        payload.put("firstName", firstName);
        payload.put("lastName", lastName);
        payload.put("title", title);
        return payload;
    }

    private Map<String, Object> buildUnlinkedPayload(UUID officerId, UUID companyId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("aggregateId", officerId.toString());
        payload.put("companyId", companyId.toString());
        return payload;
    }

    private Map<String, Object> buildUpdatedPayload(UUID officerId, String firstName, String lastName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("aggregateId", officerId.toString());
        payload.put("firstName", firstName);
        payload.put("lastName", lastName);
        return payload;
    }

    private Map<String, Object> buildDeletedPayload(UUID officerId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("aggregateId", officerId.toString());
        return payload;
    }
}
