package com.company.userservice.domain.model;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "UserId value must not be null");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId fromString(String value) {
        return new UserId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
