package com.company.companyservice.infrastructure.persistence.query;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "companies")
public class CompanyDocument {

    @Id
    private UUID id;
    private String name;
    private String registrationNumber;
    private String street;
    private String city;
    private String postalCode;
    private String country;

    @Indexed
    private UUID ownerId;
    private String ownerDisplayName;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OfficerSummaryDocument> officers = new ArrayList<>();

    public CompanyDocument() {}

    public CompanyDocument(
            UUID id,
            String name,
            String registrationNumber,
            String street,
            String city,
            String postalCode,
            String country,
            UUID ownerId,
            String ownerDisplayName,
            String status,
            Instant createdAt,
            Instant updatedAt,
            List<OfficerSummaryDocument> officers) {
        this.id = id;
        this.name = name;
        this.registrationNumber = registrationNumber;
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.ownerId = ownerId;
        this.ownerDisplayName = ownerDisplayName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.officers = officers != null ? officers : new ArrayList<>();
    }

    public UUID getId() { return id; }

    public String getName() { return name; }

    public String getRegistrationNumber() { return registrationNumber; }

    public String getStreet() { return street; }

    public String getCity() { return city; }

    public String getPostalCode() { return postalCode; }

    public String getCountry() { return country; }

    public UUID getOwnerId() { return ownerId; }

    public String getOwnerDisplayName() { return ownerDisplayName; }

    public String getStatus() { return status; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public List<OfficerSummaryDocument> getOfficers() { return officers; }
}
