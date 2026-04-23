package com.company.companyservice.presentation.kafka;

import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.OfficerSummary;
import com.company.companyservice.domain.port.infrastructure.CompanyQueryRepository;
import com.company.companyservice.infrastructure.persistence.query.ProcessedEventDocument;
import com.company.companyservice.infrastructure.persistence.query.ProcessedEventMongoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OfficerEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OfficerEventConsumer.class);

    private final CompanyQueryRepository queryRepository;
    private final ProcessedEventMongoRepository processedEvents;
    private final ObjectMapper objectMapper;

    public OfficerEventConsumer(
            CompanyQueryRepository queryRepository,
            ProcessedEventMongoRepository processedEvents,
            ObjectMapper objectMapper) {
        this.queryRepository = queryRepository;
        this.processedEvents = processedEvents;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.officer-events:officer-events}",
            groupId = "company-service-group")
    public void onMessage(String rawMessage) {
        try {
            JsonNode envelope = objectMapper.readTree(rawMessage);
            UUID eventId = UUID.fromString(envelope.get("eventId").asText());

            if (processedEvents.existsById(eventId)) {
                log.debug("Skipping already-processed officer event {}", eventId);
                return;
            }

            String eventType = envelope.get("eventType").asText();
            JsonNode payload = envelope.get("payload");

            switch (eventType) {
                case "OfficerLinkedEvent" -> handleOfficerLinked(payload);
                case "OfficerUnlinkedEvent" -> handleOfficerUnlinked(payload);
                default -> log.warn("Unknown officer event type: {}", eventType);
            }

            processedEvents.save(new ProcessedEventDocument(eventId, Instant.now()));
        } catch (Exception e) {
            log.error("Failed to process officer event", e);
            throw new RuntimeException(e);
        }
    }

    private void handleOfficerLinked(JsonNode payload) {
        UUID officerId = UUID.fromString(payload.get("officerId").asText());
        UUID companyId = UUID.fromString(payload.get("companyId").asText());
        String firstName = payload.get("firstName").asText();
        String lastName = payload.get("lastName").asText();
        String title = payload.get("title").asText();

        CompanyId id = CompanyId.of(companyId);
        Optional<CompanyFullView> existing = queryRepository.findFullById(id);

        if (existing.isEmpty()) {
            log.warn("OfficerLinkedEvent received but company {} not found in read model", companyId);
            return;
        }

        CompanyFullView current = existing.get();
        boolean alreadyPresent = current.officers().stream()
                .anyMatch(o -> o.officerId().equals(officerId));

        if (alreadyPresent) {
            log.debug("Officer {} already linked to company {} — skipping", officerId, companyId);
            return;
        }

        List<OfficerSummary> updatedOfficers = new ArrayList<>(current.officers());
        updatedOfficers.add(new OfficerSummary(officerId, firstName, lastName, title));

        CompanyFullView updated = new CompanyFullView(
                current.id(),
                current.name(),
                current.registrationNumber(),
                current.address(),
                current.ownerId(),
                current.ownerDisplayName(),
                current.status(),
                current.createdAt(),
                current.updatedAt(),
                updatedOfficers
        );
        queryRepository.save(updated);
    }

    private void handleOfficerUnlinked(JsonNode payload) {
        UUID officerId = UUID.fromString(payload.get("officerId").asText());
        UUID companyId = UUID.fromString(payload.get("companyId").asText());

        CompanyId id = CompanyId.of(companyId);
        Optional<CompanyFullView> existing = queryRepository.findFullById(id);

        if (existing.isEmpty()) {
            log.warn("OfficerUnlinkedEvent received but company {} not found in read model", companyId);
            return;
        }

        CompanyFullView current = existing.get();
        List<OfficerSummary> updatedOfficers = current.officers().stream()
                .filter(o -> !o.officerId().equals(officerId))
                .toList();

        CompanyFullView updated = new CompanyFullView(
                current.id(),
                current.name(),
                current.registrationNumber(),
                current.address(),
                current.ownerId(),
                current.ownerDisplayName(),
                current.status(),
                current.createdAt(),
                current.updatedAt(),
                updatedOfficers
        );
        queryRepository.save(updated);
    }
}
