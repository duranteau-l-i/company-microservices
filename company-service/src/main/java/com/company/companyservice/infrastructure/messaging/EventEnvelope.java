package com.company.companyservice.infrastructure.messaging;

import com.company.companyservice.domain.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String aggregateType,
        Instant timestamp,
        int version,
        Object payload
) {
    public static EventEnvelope wrap(DomainEvent event) {
        return new EventEnvelope(
                event.eventId(),
                event.eventType(),
                event.aggregateId(),
                event.aggregateType(),
                event.timestamp(),
                event.version(),
                event);
    }
}
