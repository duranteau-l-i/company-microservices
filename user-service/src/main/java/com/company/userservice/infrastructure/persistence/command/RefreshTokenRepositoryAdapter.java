package com.company.userservice.infrastructure.persistence.command;

import com.company.userservice.domain.model.RefreshToken;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.port.infrastructure.RefreshTokenRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void save(RefreshToken token) {
        jpa.save(new RefreshTokenEntity(
                UUID.randomUUID(),
                token.tokenHash(),
                token.userId().value(),
                token.email(),
                token.role().name(),
                token.expiresAt(),
                Instant.now()
        ));
    }

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(e -> new RefreshToken(
                UserId.of(e.getUserId()),
                e.getEmail(),
                Role.valueOf(e.getRole()),
                e.getTokenHash(),
                e.getExpiresAt()
        ));
    }

    @Override
    @Transactional
    public void deleteByHash(String tokenHash) {
        jpa.findByTokenHash(tokenHash).ifPresent(jpa::delete);
    }

    @Override
    @Transactional
    public void deleteAllByUserId(UserId userId) {
        jpa.deleteAllByUserId(userId.value());
    }
}
