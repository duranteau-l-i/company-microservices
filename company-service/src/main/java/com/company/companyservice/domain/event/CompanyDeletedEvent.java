package com.company.companyservice.domain.event;

import com.company.companyservice.domain.model.CompanyId;

import java.time.Instant;
import java.util.UUID;

public record CompanyDeletedEvent(
        UUID eventId,
        UUID aggregateId,
        UUID ownerId,
        Instant timestamp,
        int version
) implements DomainEvent {

    public static CompanyDeletedEvent of(CompanyId companyId, UUID ownerId, Instant timestamp) {
        return new CompanyDeletedEvent(UUID.randomUUID(), companyId.value(), ownerId, timestamp, 1);
    }

    @Override
    public String eventType() {
        return "CompanyDeletedEvent";
    }

    @Override
    public String aggregateType() {
        return "Company";
    }
}
