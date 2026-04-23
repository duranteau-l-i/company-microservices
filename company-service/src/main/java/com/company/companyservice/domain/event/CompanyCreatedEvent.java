package com.company.companyservice.domain.event;

import com.company.companyservice.domain.model.CompanyId;

import java.time.Instant;
import java.util.UUID;

public record CompanyCreatedEvent(
        UUID eventId,
        UUID aggregateId,
        String name,
        String registrationNumber,
        UUID ownerId,
        Instant timestamp,
        int version
) implements DomainEvent {

    public static CompanyCreatedEvent of(CompanyId companyId, String name, String registrationNumber, UUID ownerId, Instant timestamp) {
        return new CompanyCreatedEvent(UUID.randomUUID(), companyId.value(), name, registrationNumber, ownerId, timestamp, 1);
    }

    @Override
    public String eventType() {
        return "CompanyCreatedEvent";
    }

    @Override
    public String aggregateType() {
        return "Company";
    }
}
