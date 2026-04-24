package com.company.officerservice.domain.model;

import com.company.officerservice.domain.event.OfficerCreatedEvent;
import com.company.officerservice.domain.event.OfficerLinkedToCompanyEvent;
import com.company.officerservice.domain.event.OfficerUnlinkedFromCompanyEvent;
import com.company.officerservice.domain.event.OfficerUpdatedEvent;
import com.company.officerservice.domain.exception.DuplicateLinkException;
import com.company.officerservice.domain.exception.OfficerNotFoundException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Officer {

    private final OfficerId id;
    private String firstName;
    private String lastName;
    private final LocalDate dateOfBirth;
    private String nationality;
    private Address address;
    private String email;
    private String phone;
    private final List<CompanyLink> companyLinks;
    private final Instant createdAt;
    private Instant updatedAt;

    public Officer(OfficerId id,
                   String firstName,
                   String lastName,
                   LocalDate dateOfBirth,
                   String nationality,
                   Address address,
                   String email,
                   String phone,
                   List<CompanyLink> companyLinks,
                   Instant createdAt,
                   Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.firstName = requireNonBlank(firstName, "firstName");
        this.lastName = requireNonBlank(lastName, "lastName");
        this.dateOfBirth = Objects.requireNonNull(dateOfBirth, "dateOfBirth");
        this.nationality = requireNonBlank(nationality, "nationality");
        this.address = Objects.requireNonNull(address, "address");
        this.email = requireNonBlank(email, "email");
        this.phone = phone;
        this.companyLinks = new ArrayList<>(companyLinks);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Created create(String firstName,
                                 String lastName,
                                 LocalDate dateOfBirth,
                                 String nationality,
                                 Address address,
                                 String email,
                                 String phone) {
        Instant now = Instant.now();
        Officer officer = new Officer(
                OfficerId.generate(),
                firstName,
                lastName,
                dateOfBirth,
                nationality,
                address,
                email,
                phone,
                List.of(),
                now,
                now
        );
        OfficerCreatedEvent event = new OfficerCreatedEvent(
                UUID.randomUUID(),
                officer.id.value(),
                officer.firstName,
                officer.lastName,
                now,
                1
        );
        return new Created(officer, event);
    }

    public Updated update(String firstName,
                          String lastName,
                          String nationality,
                          Address address,
                          String email,
                          String phone) {
        this.firstName = requireNonBlank(firstName, "firstName");
        this.lastName = requireNonBlank(lastName, "lastName");
        this.nationality = requireNonBlank(nationality, "nationality");
        this.address = Objects.requireNonNull(address, "address");
        this.email = requireNonBlank(email, "email");
        this.phone = phone;
        this.updatedAt = Instant.now();
        OfficerUpdatedEvent event = new OfficerUpdatedEvent(
                UUID.randomUUID(),
                this.id.value(),
                this.firstName,
                this.lastName,
                this.updatedAt,
                1
        );
        return new Updated(this, event);
    }

    public Linked linkToCompany(CompanyLink link) {
        boolean duplicate = companyLinks.stream()
                .filter(CompanyLink::active)
                .anyMatch(l -> l.companyId().equals(link.companyId()) && l.title().equals(link.title()));
        if (duplicate) {
            throw new DuplicateLinkException(link.companyId(), link.title());
        }
        companyLinks.add(link);
        this.updatedAt = Instant.now();
        OfficerLinkedToCompanyEvent event = new OfficerLinkedToCompanyEvent(
                UUID.randomUUID(),
                this.id.value(),
                link.companyId(),
                link.title(),
                this.firstName,
                this.lastName,
                this.updatedAt,
                1
        );
        return new Linked(this, event);
    }

    public Unlinked unlinkFromCompany(UUID companyId) {
        CompanyLink link = companyLinks.stream()
                .filter(CompanyLink::active)
                .filter(l -> l.companyId().equals(companyId))
                .findFirst()
                .orElseThrow(() -> new OfficerNotFoundException(companyId.toString()));
        link.resign(LocalDate.now());
        this.updatedAt = Instant.now();
        OfficerUnlinkedFromCompanyEvent event = new OfficerUnlinkedFromCompanyEvent(
                UUID.randomUUID(),
                this.id.value(),
                companyId,
                this.updatedAt,
                1
        );
        return new Unlinked(this, event);
    }

    public OfficerId id() { return id; }

    public String firstName() { return firstName; }

    public String lastName() { return lastName; }

    public LocalDate dateOfBirth() { return dateOfBirth; }

    public String nationality() { return nationality; }

    public Address address() { return address; }

    public String email() { return email; }

    public String phone() { return phone; }

    public List<CompanyLink> companyLinks() { return Collections.unmodifiableList(companyLinks); }

    public Instant createdAt() { return createdAt; }

    public Instant updatedAt() { return updatedAt; }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public record Created(Officer officer, OfficerCreatedEvent event) {}

    public record Updated(Officer officer, OfficerUpdatedEvent event) {}

    public record Linked(Officer officer, OfficerLinkedToCompanyEvent event) {}

    public record Unlinked(Officer officer, OfficerUnlinkedFromCompanyEvent event) {}
}
