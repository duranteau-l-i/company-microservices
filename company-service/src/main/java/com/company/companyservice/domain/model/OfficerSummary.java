package com.company.companyservice.domain.model;

import java.util.Objects;
import java.util.UUID;

public record OfficerSummary(UUID officerId, String firstName, String lastName, String title) {

    public OfficerSummary {
        Objects.requireNonNull(officerId, "officerId must not be null");
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("firstName must not be blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("lastName must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
    }
}
