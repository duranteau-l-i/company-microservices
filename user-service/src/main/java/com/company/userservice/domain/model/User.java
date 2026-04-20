package com.company.userservice.domain.model;

import com.company.userservice.domain.event.UserCreatedEvent;

import java.time.Instant;
import java.util.Objects;

public final class User {

    private final UserId id;
    private final EmailAddress email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private Role role;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public User(UserId id,
                EmailAddress email,
                String passwordHash,
                String firstName,
                String lastName,
                Role role,
                boolean active,
                Instant createdAt,
                Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash");
        this.firstName = requireNonBlank(firstName, "firstName");
        this.lastName = requireNonBlank(lastName, "lastName");
        this.role = Objects.requireNonNull(role, "role");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Created create(EmailAddress email,
                                  String passwordHash,
                                  String firstName,
                                  String lastName,
                                  Role role) {
        Instant now = Instant.now();
        User user = new User(UserId.generate(), email, passwordHash, firstName, lastName, role, true, now, now);
        UserCreatedEvent event = UserCreatedEvent.of(user.id, user.email, user.role, now);
        return new Created(user, event);
    }

    public void updateProfile(String firstName, String lastName) {
        this.firstName = requireNonBlank(firstName, "firstName");
        this.lastName = requireNonBlank(lastName, "lastName");
        this.updatedAt = Instant.now();
    }

    public void changeRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "role");
        this.updatedAt = Instant.now();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = requireNonBlank(newPasswordHash, "passwordHash");
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public UserId id() { return id; }
    public EmailAddress email() { return email; }
    public String passwordHash() { return passwordHash; }
    public String firstName() { return firstName; }
    public String lastName() { return lastName; }
    public Role role() { return role; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public record Created(User user, UserCreatedEvent event) {}
}
