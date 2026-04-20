package com.company.userservice.domain.model;

import java.time.Instant;

public record UserReadModel(
        UserId id,
        EmailAddress email,
        String firstName,
        String lastName,
        Role role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserReadModel from(User user) {
        return new UserReadModel(
                user.id(),
                user.email(),
                user.firstName(),
                user.lastName(),
                user.role(),
                user.active(),
                user.createdAt(),
                user.updatedAt());
    }
}
