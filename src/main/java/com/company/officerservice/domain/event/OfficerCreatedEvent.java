package com.company.officerservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record OfficerCreatedEvent(
        UUID eventId,
        UUID aggregateId,
        String firstName,
        String lastName,
        Instant timestamp,
        int version
) implements DomainEvent {

    @Override
    public String eventType() {
        return "OfficerCreatedEvent";
    }

    @Override
    public String aggregateType() {
        return "Officer";
    }
}
