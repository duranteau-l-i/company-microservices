package com.company.officerservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record OfficerDeletedEvent(
        UUID eventId,
        UUID aggregateId,
        Instant timestamp,
        int version
) implements DomainEvent {

    public static OfficerDeletedEvent of(UUID officerId) {
        return new OfficerDeletedEvent(UUID.randomUUID(), officerId, Instant.now(), 1);
    }

    @Override
    public String eventType() { return "OfficerDeletedEvent"; }

    @Override
    public String aggregateType() { return "Officer"; }
}
