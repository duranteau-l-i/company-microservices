package com.company.officerservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record OfficerUnlinkedFromCompanyEvent(
        UUID eventId,
        UUID aggregateId,
        UUID companyId,
        Instant timestamp,
        int version
) implements DomainEvent {

    @Override
    public String eventType() {
        return "OfficerUnlinkedFromCompanyEvent";
    }

    @Override
    public String aggregateType() {
        return "Officer";
    }
}
