package com.company.officerservice.domain.model;

import java.util.Objects;
import java.util.UUID;

public record OfficerId(UUID value) {

    public OfficerId {
        Objects.requireNonNull(value, "OfficerId value must not be null");
    }

    public static OfficerId generate() {
        return new OfficerId(UUID.randomUUID());
    }

    public static OfficerId of(UUID value) {
        return new OfficerId(value);
    }

    public static OfficerId fromString(String value) {
        return new OfficerId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
