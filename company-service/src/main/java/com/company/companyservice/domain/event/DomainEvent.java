package com.company.companyservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();

    String eventType();

    UUID aggregateId();

    String aggregateType();

    Instant timestamp();

    int version();
}
