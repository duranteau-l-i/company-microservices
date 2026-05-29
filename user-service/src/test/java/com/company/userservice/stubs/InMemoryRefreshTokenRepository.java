package com.company.userservice.stubs;

import com.company.userservice.domain.model.RefreshToken;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.port.infrastructure.RefreshTokenRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final Map<String, RefreshToken> byHash = new HashMap<>();

    @Override
    public void save(RefreshToken token) {
        byHash.put(token.tokenHash(), token);
    }

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
        return Optional.ofNullable(byHash.get(tokenHash));
    }

    @Override
    public void deleteByHash(String tokenHash) {
        byHash.remove(tokenHash);
    }

    @Override
    public void deleteAllByUserId(UserId userId) {
        byHash.values().removeIf(t -> t.userId().equals(userId));
    }

    public int size() {
        return byHash.size();
    }

    public void clear() {
        byHash.clear();
    }
}
