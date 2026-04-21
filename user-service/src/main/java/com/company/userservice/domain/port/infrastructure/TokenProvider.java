package com.company.userservice.domain.port.infrastructure;

import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;

public interface TokenProvider {
    TokenPair issueTokens(UserId userId, String email, Role role);

    TokenPair refresh(String refreshToken);

    record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {}
}
