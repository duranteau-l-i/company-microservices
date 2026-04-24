package com.company.officerservice.infrastructure.persistence.query;

import java.time.LocalDate;
import java.util.UUID;

public class CompanyLinkDocument {

    private UUID companyId;
    private String title;
    private LocalDate appointmentDate;
    private LocalDate resignationDate;
    private boolean active;

    public CompanyLinkDocument() {}

    public CompanyLinkDocument(UUID companyId, String title, LocalDate appointmentDate,
                               LocalDate resignationDate, boolean active) {
        this.companyId = companyId;
        this.title = title;
        this.appointmentDate = appointmentDate;
        this.resignationDate = resignationDate;
        this.active = active;
    }

    public UUID getCompanyId() { return companyId; }
    public String getTitle() { return title; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public LocalDate getResignationDate() { return resignationDate; }
    public boolean isActive() { return active; }
}
