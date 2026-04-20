package com.company.userservice.domain.event;

import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;

import java.time.Instant;
import java.util.UUID;

public record UserUpdatedEvent(
        UUID eventId,
        UUID aggregateId,
        String email,
        String firstName,
        String lastName,
        String role,
        Instant timestamp,
        int version
) implements DomainEvent {

    public static UserUpdatedEvent of(UserId userId,
                                      EmailAddress email,
                                      String firstName,
                                      String lastName,
                                      Role role,
                                      Instant timestamp) {
        return new UserUpdatedEvent(UUID.randomUUID(), userId.value(), email.value(), firstName, lastName, role.name(), timestamp, 1);
    }

    @Override
    public String eventType() {
        return "UserUpdatedEvent";
    }

    @Override
    public String aggregateType() {
        return "User";
    }
}