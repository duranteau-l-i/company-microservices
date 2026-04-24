package com.company.officerservice.domain.event;

import com.company.officerservice.domain.model.Officer;

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

    public static OfficerLinkedToCompanyEvent of(Officer officer, UUID companyId, String title) {
        return new OfficerLinkedToCompanyEvent(
                UUID.randomUUID(),
                officer.id().value(),
                companyId,
                title,
                officer.firstName(),
                officer.lastName(),
                Instant.now(),
                1
        );
    }

    @Override
    public String eventType() {
        return "OfficerLinkedToCompanyEvent";
    }

    @Override
    public String aggregateType() {
        return "Officer";
    }
}
