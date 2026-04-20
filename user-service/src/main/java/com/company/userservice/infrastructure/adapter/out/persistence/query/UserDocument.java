package com.company.userservice.infrastructure.adapter.out.persistence.query;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "users")
public class UserDocument {

    @Id
    private UUID id;

    @Indexed(unique = true)
    private String email;

    private String firstName;
    private String lastName;
    private String role;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public UserDocument() {}

    public UserDocument(
            UUID id,
            String email,
            String firstName,
            String lastName,
            String role,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
