package com.company.companyservice.infrastructure.persistence.query;

import java.util.UUID;

public class OfficerSummaryDocument {

    private UUID officerId;
    private String firstName;
    private String lastName;
    private String title;

    public OfficerSummaryDocument() {}

    public OfficerSummaryDocument(UUID officerId, String firstName, String lastName, String title) {
        this.officerId = officerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.title = title;
    }

    public UUID getOfficerId() { return officerId; }

    public String getFirstName() { return firstName; }

    public String getLastName() { return lastName; }

    public String getTitle() { return title; }
}
