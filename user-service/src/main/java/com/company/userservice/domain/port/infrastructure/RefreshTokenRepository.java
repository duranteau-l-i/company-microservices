package com.company.userservice.domain.port.infrastructure;

import com.company.userservice.domain.model.RefreshToken;
import com.company.userservice.domain.model.UserId;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(RefreshToken token);
    Optional<RefreshToken> findByHash(String tokenHash);
    void deleteByHash(String tokenHash);
    void deleteAllByUserId(UserId userId);
}
