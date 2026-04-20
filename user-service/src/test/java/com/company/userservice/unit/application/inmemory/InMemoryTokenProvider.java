package com.company.userservice.unit.application.inmemory;

import com.company.userservice.domain.exception.InvalidCredentialsException;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.port.out.TokenProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemoryTokenProvider implements TokenProvider {

    private final Map<String, UserId> refreshTokens = new HashMap<>();

    @Override
    public TokenPair issueTokens(UserId userId, String email, Role role) {
        String access = "access-" + userId.value() + "-" + UUID.randomUUID();
        String refresh = "refresh-" + userId.value() + "-" + UUID.randomUUID();
        refreshTokens.put(refresh, userId);
        return new TokenPair(access, refresh, 1800L);
    }

    @Override
    public TokenPair refresh(String refreshToken) {
        UserId userId = refreshTokens.remove(refreshToken);
        if (userId == null) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }
        String access = "access-" + userId.value() + "-" + UUID.randomUUID();
        String newRefresh = "refresh-" + userId.value() + "-" + UUID.randomUUID();
        refreshTokens.put(newRefresh, userId);
        return new TokenPair(access, newRefresh, 1800L);
    }

    public void clear() {
        refreshTokens.clear();
    }
}
