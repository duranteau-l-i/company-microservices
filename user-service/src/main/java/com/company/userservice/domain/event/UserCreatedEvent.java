package com.company.userservice.domain.event;

import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;

import java.time.Instant;
import java.util.UUID;

public record UserCreatedEvent(
        UUID eventId,
        UUID aggregateId,
        String email,
        String role,
        Instant timestamp,
        int version
) implements DomainEvent {

    public static UserCreatedEvent of(UserId userId, EmailAddress email, Role role, Instant timestamp) {
        return new UserCreatedEvent(UUID.randomUUID(), userId.value(), email.value(), role.name(), timestamp, 1);
    }

    @Override
    public String eventType() {
        return "UserCreatedEvent";
    }

    @Override
    public String aggregateType() {
        return "User";
    }
}