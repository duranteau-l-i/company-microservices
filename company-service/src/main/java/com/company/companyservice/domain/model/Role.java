package com.company.companyservice.domain.model;

public enum Role {
    ADMIN,
    MANAGER,
    USER;

    public boolean isAtLeast(Role other) {
        return this.ordinal() <= other.ordinal();
    }
}
