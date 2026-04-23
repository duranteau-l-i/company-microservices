package com.company.companyservice.domain.event;

import com.company.companyservice.domain.model.CompanyId;

import java.time.Instant;
import java.util.UUID;

public record CompanyUpdatedEvent(
        UUID eventId,
        UUID aggregateId,
        String name,
        String registrationNumber,
        Instant timestamp,
        int version
) implements DomainEvent {

    public static CompanyUpdatedEvent of(CompanyId companyId, String name, String registrationNumber, Instant timestamp) {
        return new CompanyUpdatedEvent(UUID.randomUUID(), companyId.value(), name, registrationNumber, timestamp, 1);
    }

    @Override
    public String eventType() {
        return "CompanyUpdatedEvent";
    }

    @Override
    public String aggregateType() {
        return "Company";
    }
}
