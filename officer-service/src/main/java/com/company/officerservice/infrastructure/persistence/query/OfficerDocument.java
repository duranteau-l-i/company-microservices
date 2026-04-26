package com.company.officerservice.infrastructure.persistence.query;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "officers")
public class OfficerDocument {

    @Id
    private UUID id;
    private String firstName;
    private String lastName;

    @Indexed
    private LocalDate dateOfBirth;
    private String nationality;
    private String street;
    private String city;
    private String postalCode;
    private String country;
    private String email;
    private String phone;
    private List<CompanyLinkDocument> companyLinks = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public OfficerDocument() {}

    public OfficerDocument(UUID id, String firstName, String lastName, LocalDate dateOfBirth,
                           String nationality, String street, String city, String postalCode,
                           String country, String email, String phone,
                           List<CompanyLinkDocument> companyLinks, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.email = email;
        this.phone = phone;
        this.companyLinks = companyLinks != null ? companyLinks : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getNationality() { return nationality; }
    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public List<CompanyLinkDocument> getCompanyLinks() { return companyLinks; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
