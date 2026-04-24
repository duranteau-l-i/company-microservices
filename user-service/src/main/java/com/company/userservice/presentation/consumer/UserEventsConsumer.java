package com.company.userservice.presentation.consumer;

import com.company.userservice.domain.event.UserCreatedEvent;
import com.company.userservice.domain.event.UserDeletedEvent;
import com.company.userservice.domain.event.UserUpdatedEvent;
import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.infrastructure.UserQueryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventsConsumer.class);

    private final UserQueryRepository queryRepository;
    private final ProcessedEventRepository processedEvents;
    private final ObjectMapper objectMapper;

    public UserEventsConsumer(
            UserQueryRepository queryRepository,
            ProcessedEventRepository processedEvents,
            ObjectMapper objectMapper) {
        this.queryRepository = queryRepository;
        this.processedEvents = processedEvents;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.user-events:user-events}",
            groupId = "${spring.application.name:user-service}-group")
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
                case "UserCreatedEvent" -> handleCreated(objectMapper.treeToValue(payload, UserCreatedEvent.class));
                case "UserUpdatedEvent" -> handleUpdated(objectMapper.treeToValue(payload, UserUpdatedEvent.class));
                case "UserDeletedEvent" -> handleDeleted(objectMapper.treeToValue(payload, UserDeletedEvent.class));
                default -> log.warn("Unknown event type: {}", eventType);
            }

            processedEvents.save(new ProcessedEventDocument(eventId, eventType, Instant.now()));
        } catch (Exception e) {
            log.error("Failed to process user event", e);
            throw new RuntimeException(e);
        }
    }

    private void handleCreated(UserCreatedEvent event) {
        UserReadModel model = new UserReadModel(
                UserId.of(event.aggregateId()),
                EmailAddress.of(event.email()),
                "",
                "",
                Role.valueOf(event.role()),
                true,
                event.timestamp(),
                event.timestamp());

        queryRepository.save(model);
    }

    private void handleUpdated(UserUpdatedEvent event) {
        Optional<UserReadModel> existing = queryRepository.findById(UserId.of(event.aggregateId()));

        Instant createdAt = existing.map(UserReadModel::createdAt).orElse(event.timestamp());
        boolean active = existing.map(UserReadModel::active).orElse(true);

        UserReadModel updated = new UserReadModel(
                UserId.of(event.aggregateId()),
                EmailAddress.of(event.email()),
                event.firstName(),
                event.lastName(),
                Role.valueOf(event.role()),
                active,
                createdAt,
                event.timestamp());

        queryRepository.save(updated);
    }

    private void handleDeleted(UserDeletedEvent event) {
        queryRepository.deleteById(UserId.of(event.aggregateId()));
    }
}
