package com.company.userservice.domain.model;

import java.time.Instant;

public record RefreshToken(
        UserId userId,
        String email,
        Role role,
        String tokenHash,
        Instant expiresAt
) {
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
