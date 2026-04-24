package com.company.officerservice.domain.model;

public enum Role {
    ADMIN,
    MANAGER,
    USER;

    public boolean isAtLeast(Role other) {
        return this.ordinal() <= other.ordinal();
    }
}
