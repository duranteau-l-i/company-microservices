package com.company.officerservice.domain.event;

import com.company.officerservice.domain.model.Officer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OfficerCreatedEvent(
        UUID eventId,
        UUID aggregateId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String nationality,
        String email,
        String phone,
        Instant timestamp,
        int version
) implements DomainEvent {

    public static OfficerCreatedEvent of(Officer officer) {
        return new OfficerCreatedEvent(
                UUID.randomUUID(),
                officer.id().value(),
                officer.firstName(),
                officer.lastName(),
                officer.dateOfBirth(),
                officer.nationality(),
                officer.email(),
                officer.phone(),
                Instant.now(),
                1
        );
    }

    @Override
    public String eventType() {
        return "OfficerCreatedEvent";
    }

    @Override
    public String aggregateType() {
        return "Officer";
    }
}
