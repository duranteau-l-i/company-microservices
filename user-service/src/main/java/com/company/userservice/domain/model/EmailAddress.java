package com.company.userservice.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record EmailAddress(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public EmailAddress {
        Objects.requireNonNull(value, "Email must not be null");
        String trimmed = value.trim().toLowerCase();
        if (trimmed.isEmpty() || !EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid email address: " + value);
        }
        value = trimmed;
    }

    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
