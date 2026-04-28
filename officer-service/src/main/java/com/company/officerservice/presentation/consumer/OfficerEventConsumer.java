package com.company.officerservice.presentation.consumer;

import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.model.OfficerId;
import com.company.officerservice.domain.port.infrastructure.OfficerCommandRepository;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import com.company.officerservice.infrastructure.persistence.query.ProcessedEventDocument;
import com.company.officerservice.infrastructure.persistence.query.ProcessedEventDocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OfficerEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OfficerEventConsumer.class);

    private final OfficerCommandRepository commandRepository;
    private final OfficerQueryRepository queryRepository;
    private final ProcessedEventDocumentRepository processedEvents;
    private final ObjectMapper objectMapper;

    public OfficerEventConsumer(
            OfficerCommandRepository commandRepository,
            OfficerQueryRepository queryRepository,
            ProcessedEventDocumentRepository processedEvents,
            ObjectMapper objectMapper) {
        this.commandRepository = commandRepository;
        this.queryRepository = queryRepository;
        this.processedEvents = processedEvents;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.officer-events:officer-events}",
            groupId = "officer-service-group")
    public void onMessage(String rawMessage) {
        try {
            JsonNode envelope = objectMapper.readTree(rawMessage);
            UUID eventId = UUID.fromString(envelope.get("eventId").asText());

            if (processedEvents.existsById(eventId)) {
                log.debug("Skipping already-processed officer event {}", eventId);
                return;
            }

            String eventType = envelope.get("eventType").asText();
            UUID aggregateId = UUID.fromString(envelope.get("aggregateId").asText());

            switch (eventType) {
                case "OfficerCreatedEvent",
                     "OfficerUpdatedEvent",
                     "OfficerLinkedToCompanyEvent",
                     "OfficerUnlinkedFromCompanyEvent" -> syncReadModel(aggregateId);
                case "OfficerDeletedEvent" -> handleDeleted(aggregateId);
                default -> log.warn("Unknown officer event type: {}", eventType);
            }

            processedEvents.save(new ProcessedEventDocument(eventId, Instant.now()));
        } catch (Exception e) {
            log.error("Failed to process officer event", e);
            throw new RuntimeException(e);
        }
    }

    private void syncReadModel(UUID officerId) {
        OfficerId id = OfficerId.of(officerId);
        commandRepository.findById(id).ifPresentOrElse(
                officer -> {
                    OfficerFullView view = new OfficerFullView(
                            officer.id(), officer.firstName(), officer.lastName(), officer.dateOfBirth(),
                            officer.nationality(), officer.address(), officer.email(), officer.phone(),
                            officer.companyLinks(), officer.createdAt(), officer.updatedAt()
                    );
                    queryRepository.save(view);
                },
                () -> log.warn("Officer {} not found in command store during read model sync", officerId)
        );
    }

    private void handleDeleted(UUID officerId) {
        queryRepository.deleteById(OfficerId.of(officerId));
    }
}
