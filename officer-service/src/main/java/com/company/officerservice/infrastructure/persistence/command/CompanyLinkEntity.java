package com.company.officerservice.infrastructure.persistence.command;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "company_links")
public class CompanyLinkEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", nullable = false)
    private OfficerEntity officer;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String title;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "resignation_date")
    private LocalDate resignationDate;

    @Column(nullable = false)
    private boolean active;

    protected CompanyLinkEntity() {}

    public CompanyLinkEntity(UUID id, OfficerEntity officer, UUID companyId,
                                String title, LocalDate appointmentDate,
                                LocalDate resignationDate, boolean active) {
        this.id = id;
        this.officer = officer;
        this.companyId = companyId;
        this.title = title;
        this.appointmentDate = appointmentDate;
        this.resignationDate = resignationDate;
        this.active = active;
    }

    public UUID getId() { return id; }
    public OfficerEntity getOfficer() { return officer; }
    public UUID getCompanyId() { return companyId; }
    public String getTitle() { return title; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public LocalDate getResignationDate() { return resignationDate; }
    public boolean isActive() { return active; }
}
