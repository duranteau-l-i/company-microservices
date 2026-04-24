package com.company.officerservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record OfficerUpdatedEvent(
        UUID eventId,
        UUID aggregateId,
        String firstName,
        String lastName,
        Instant timestamp,
        int version
) implements DomainEvent {

    @Override
    public String eventType() {
        return "OfficerUpdatedEvent";
    }

    @Override
    public String aggregateType() {
        return "Officer";
    }
}
