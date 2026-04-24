package com.company.officerservice.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class CompanyLink {

    private final UUID companyId;
    private final String title;
    private final LocalDate appointmentDate;
    private LocalDate resignationDate;
    private boolean active;

    public CompanyLink(UUID companyId, String title, LocalDate appointmentDate, LocalDate resignationDate, boolean active) {
        this.companyId = Objects.requireNonNull(companyId, "companyId must not be null");
        this.title = requireNonBlank(title, "title");
        this.appointmentDate = Objects.requireNonNull(appointmentDate, "appointmentDate must not be null");
        this.resignationDate = resignationDate;
        this.active = active;
    }

    public static CompanyLink create(UUID companyId, String title, LocalDate appointmentDate) {
        return new CompanyLink(companyId, title, appointmentDate, null, true);
    }

    public void resign(LocalDate resignationDate) {
        this.resignationDate = resignationDate;
        this.active = false;
    }

    public UUID companyId() { return companyId; }

    public String title() { return title; }

    public LocalDate appointmentDate() { return appointmentDate; }

    public LocalDate resignationDate() { return resignationDate; }

    public boolean active() { return active; }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
