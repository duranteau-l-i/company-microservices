package com.company.userservice.infrastructure.adapter.in.rest.dto;

import com.company.userservice.domain.model.UserReadModel;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(UserReadModel model) {
        return new UserResponse(
                model.id().value(),
                model.email().value(),
                model.firstName(),
                model.lastName(),
                model.role().name(),
                model.active(),
                model.createdAt(),
                model.updatedAt());
    }
}
