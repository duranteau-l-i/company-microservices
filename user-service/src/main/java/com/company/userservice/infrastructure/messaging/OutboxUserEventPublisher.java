package com.company.userservice.infrastructure.messaging;

import com.company.userservice.domain.event.DomainEvent;
import com.company.userservice.domain.port.infrastructure.UserEventPublisher;
import com.company.userservice.infrastructure.persistence.command.OutboxEventEntity;
import com.company.userservice.infrastructure.persistence.command.OutboxEventEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Primary
@Component
public class OutboxUserEventPublisher implements UserEventPublisher {

    private final OutboxEventEntityRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OutboxUserEventPublisher(
            OutboxEventEntityRepository outboxRepository,
            ObjectMapper kafkaObjectMapper,
            @Value("${app.kafka.topics.user-events:user-events}") String topic) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = kafkaObjectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            EventEnvelope envelope = EventEnvelope.wrap(event);
            String payload = objectMapper.writeValueAsString(envelope);
            OutboxEventEntity entity = new OutboxEventEntity(
                    event.eventId(),
                    event.aggregateId(),
                    event.eventType(),
                    topic,
                    payload,
                    "PENDING",
                    Instant.now(),
                    null
            );
            outboxRepository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write event to outbox", e);
        }
    }
}
