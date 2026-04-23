package com.company.companyservice.presentation.kafka;

import com.company.companyservice.domain.event.CompanyCreatedEvent;
import com.company.companyservice.domain.event.CompanyDeletedEvent;
import com.company.companyservice.domain.event.CompanyUpdatedEvent;
import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyStatus;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CompanyEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CompanyEventConsumer.class);

    private final CompanyQueryRepository queryRepository;
    private final ProcessedEventMongoRepository processedEvents;
    private final ObjectMapper objectMapper;

    public CompanyEventConsumer(
            CompanyQueryRepository queryRepository,
            ProcessedEventMongoRepository processedEvents,
            ObjectMapper objectMapper) {
        this.queryRepository = queryRepository;
        this.processedEvents = processedEvents;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.company-events:company-events}",
            groupId = "company-service-group")
    public void onMessage(String rawMessage) {
        try {
            JsonNode envelope = objectMapper.readTree(rawMessage);
            UUID eventId = UUID.fromString(envelope.get("eventId").asText());

            if (processedEvents.existsById(eventId)) {
                log.debug("Skipping already-processed event {}", eventId);
                return;
            }

            String eventType = envelope.get("eventType").asText();
            JsonNode payload = envelope.get("payload");

            switch (eventType) {
                case "CompanyCreatedEvent" -> handleCreated(
                        objectMapper.treeToValue(payload, CompanyCreatedEvent.class));
                case "CompanyUpdatedEvent" -> handleUpdated(
                        objectMapper.treeToValue(payload, CompanyUpdatedEvent.class));
                case "CompanyDeletedEvent" -> handleDeleted(
                        objectMapper.treeToValue(payload, CompanyDeletedEvent.class));
                default -> log.warn("Unknown company event type: {}", eventType);
            }

            processedEvents.save(new ProcessedEventDocument(eventId, Instant.now()));
        } catch (Exception e) {
            log.error("Failed to process company event", e);
            throw new RuntimeException(e);
        }
    }

    private void handleCreated(CompanyCreatedEvent event) {
        // ownerDisplayName and address are not in the event — use empty defaults.
        // The command handler already saves the authoritative version directly to MongoDB.
        // This handler enables read-model rebuild from the event log.
        // ownerDisplayName and address are not in the event.
        // The command handler already saves the authoritative version directly to MongoDB.
        // This handler enables read-model rebuild from the event log.
        // Use placeholder address fields if not already present.
        CompanyId id = CompanyId.of(event.aggregateId());
        Optional<CompanyFullView> existing = queryRepository.findFullById(id);
        Address address = existing.map(CompanyFullView::address)
                .orElse(new Address("unknown", "unknown", "00000", "unknown"));
        String ownerDisplayName = existing.map(CompanyFullView::ownerDisplayName).orElse("");

        CompanyFullView view = new CompanyFullView(
                id,
                event.name(),
                event.registrationNumber(),
                address,
                event.ownerId(),
                ownerDisplayName,
                CompanyStatus.ACTIVE,
                event.timestamp(),
                event.timestamp(),
                List.of()
        );
        queryRepository.save(view);
    }

    private void handleUpdated(CompanyUpdatedEvent event) {
        CompanyId id = CompanyId.of(event.aggregateId());
        Optional<CompanyFullView> existing = queryRepository.findFullById(id);

        if (existing.isEmpty()) {
            log.warn("CompanyUpdatedEvent received but company {} not found in read model", id.value());
            return;
        }

        CompanyFullView current = existing.get();
        CompanyFullView updated = new CompanyFullView(
                current.id(),
                event.name(),
                event.registrationNumber(),
                current.address(),
                current.ownerId(),
                current.ownerDisplayName(),
                current.status(),
                current.createdAt(),
                event.timestamp(),
                current.officers()
        );
        queryRepository.save(updated);
    }

    private void handleDeleted(CompanyDeletedEvent event) {
        queryRepository.deleteById(CompanyId.of(event.aggregateId()));
    }
}
