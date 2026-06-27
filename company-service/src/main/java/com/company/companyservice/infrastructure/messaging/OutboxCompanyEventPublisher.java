package com.company.companyservice.infrastructure.messaging;

import com.company.companyservice.domain.event.DomainEvent;
import com.company.companyservice.domain.port.infrastructure.CompanyEventPublisher;
import com.company.companyservice.infrastructure.persistence.command.OutboxEventEntity;
import com.company.companyservice.infrastructure.persistence.command.OutboxEventEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Primary
@Component
public class OutboxCompanyEventPublisher implements CompanyEventPublisher {

    private final OutboxEventEntityRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OutboxCompanyEventPublisher(
            OutboxEventEntityRepository outboxRepository,
            ObjectMapper kafkaObjectMapper,
            @Value("${app.kafka.topics.company-events:company-events}") String topic) {
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
