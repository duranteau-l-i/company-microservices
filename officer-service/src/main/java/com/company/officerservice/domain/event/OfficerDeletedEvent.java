package com.company.officerservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record OfficerDeletedEvent(
        UUID eventId,
        UUID aggregateId,
        String firstName,
        String lastName,
        Instant timestamp,
        int version
) implements DomainEvent {

    public static OfficerDeletedEvent of(UUID officerId, String firstName, String lastName) {
        return new OfficerDeletedEvent(UUID.randomUUID(), officerId, firstName, lastName, Instant.now(), 1);
    }

    @Override
    public String eventType() { return "OfficerDeletedEvent"; }

    @Override
    public String aggregateType() { return "Officer"; }
}
