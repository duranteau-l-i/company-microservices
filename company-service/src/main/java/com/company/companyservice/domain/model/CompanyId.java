package com.company.companyservice.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CompanyId(UUID value) {

    public CompanyId {
        Objects.requireNonNull(value, "CompanyId value must not be null");
    }

    public static CompanyId generate() {
        return new CompanyId(UUID.randomUUID());
    }

    public static CompanyId of(UUID value) {
        return new CompanyId(value);
    }

    public static CompanyId fromString(String value) {
        return new CompanyId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
