package com.company.userservice.domain.model;

import java.util.Set;

public enum Role {
    ADMIN(Set.of("MANAGER", "USER")),
    MANAGER(Set.of("USER")),
    USER(Set.of());

    private final Set<String> creatableRoles;

    Role(Set<String> creatableRoles) {
        this.creatableRoles = creatableRoles;
    }

    public boolean canCreate(Role target) {
        return creatableRoles.contains(target.name());
    }

    public boolean isAtLeast(Role other) {
        return this.ordinal() <= other.ordinal();
    }
}
