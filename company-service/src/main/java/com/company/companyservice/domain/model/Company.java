package com.company.companyservice.domain.model;

import com.company.companyservice.domain.event.CompanyCreatedEvent;
import com.company.companyservice.domain.event.CompanyUpdatedEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Company {

    private final CompanyId id;
    private String name;
    private String registrationNumber;
    private Address address;
    private final UUID ownerId;
    private CompanyStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Company(CompanyId id,
                   String name,
                   String registrationNumber,
                   Address address,
                   UUID ownerId,
                   CompanyStatus status,
                   Instant createdAt,
                   Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = requireNonBlank(name, "name");
        this.registrationNumber = requireNonBlank(registrationNumber, "registrationNumber");
        this.address = Objects.requireNonNull(address, "address");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Created create(String name, String registrationNumber, Address address, UUID ownerId) {
        Instant now = Instant.now();
        Company company = new Company(
                CompanyId.generate(),
                name,
                registrationNumber,
                address,
                ownerId,
                CompanyStatus.ACTIVE,
                now,
                now
        );
        CompanyCreatedEvent event = CompanyCreatedEvent.of(company.id, company.name, company.registrationNumber, company.ownerId, now);
        return new Created(company, event);
    }

    public Updated update(String name, String registrationNumber, Address address) {
        this.name = requireNonBlank(name, "name");
        this.registrationNumber = requireNonBlank(registrationNumber, "registrationNumber");
        this.address = Objects.requireNonNull(address, "address");
        this.updatedAt = Instant.now();
        CompanyUpdatedEvent event = CompanyUpdatedEvent.of(this.id, this.name, this.registrationNumber, this.updatedAt);
        return new Updated(event);
    }

    public void activate() {
        this.status = CompanyStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = CompanyStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public CompanyId id() { return id; }

    public String name() { return name; }

    public String registrationNumber() { return registrationNumber; }

    public Address address() { return address; }

    public UUID ownerId() { return ownerId; }

    public CompanyStatus status() { return status; }

    public Instant createdAt() { return createdAt; }

    public Instant updatedAt() { return updatedAt; }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public record Created(Company company, CompanyCreatedEvent event) {}

    public record Updated(CompanyUpdatedEvent event) {}
}
