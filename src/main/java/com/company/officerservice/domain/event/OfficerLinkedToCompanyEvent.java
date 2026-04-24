package com.company.officerservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record OfficerLinkedToCompanyEvent(
        UUID eventId,
        UUID aggregateId,
        UUID companyId,
        String title,
        String firstName,
        String lastName,
        Instant timestamp,
        int version
) implements DomainEvent {

    @Override
    public String eventType() {
        return "OfficerLinkedToCompanyEvent";
    }

    @Override
    public String aggregateType() {
        return "Officer";
    }
}
