package com.company.officerservice.domain.event;

import com.company.officerservice.domain.model.Officer;

import java.time.Instant;
import java.util.UUID;

public record OfficerUnlinkedFromCompanyEvent(
        UUID eventId,
        UUID aggregateId,
        UUID companyId,
        Instant timestamp,
        int version
) implements DomainEvent {

    public static OfficerUnlinkedFromCompanyEvent of(Officer officer, UUID companyId) {
        return new OfficerUnlinkedFromCompanyEvent(
                UUID.randomUUID(),
                officer.id().value(),
                companyId,
                Instant.now(),
                1
        );
    }

    @Override
    public String eventType() {
        return "OfficerUnlinkedFromCompanyEvent";
    }

    @Override
    public String aggregateType() {
        return "Officer";
    }
}
