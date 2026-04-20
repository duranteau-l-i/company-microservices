package com.company.userservice.domain.event;

import com.company.userservice.domain.model.UserId;

import java.time.Instant;
import java.util.UUID;

public record UserDeletedEvent(
        UUID eventId,
        UUID aggregateId,
        Instant timestamp,
        int version
) implements DomainEvent {

    public static UserDeletedEvent of(UserId userId, Instant timestamp) {
        return new UserDeletedEvent(UUID.randomUUID(), userId.value(), timestamp, 1);
    }

    @Override
    public String eventType() {
        return "UserDeletedEvent";
    }

    @Override
    public String aggregateType() {
        return "User";
    }
}