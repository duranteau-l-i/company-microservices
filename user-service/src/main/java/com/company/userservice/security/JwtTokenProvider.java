package com.company.userservice.security;

import com.company.userservice.domain.exception.InvalidCredentialsException;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.port.infrastructure.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider implements TokenProvider {

    private final SecretKey signingKey;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.access-token-expiration:1800000}") long accessTtlMillis,
            @Value("${app.security.jwt.refresh-token-expiration:604800000}") long refreshTtlMillis) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.security.jwt.secret must be at least 32 characters");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlMillis / 1000;
        this.refreshTtlSeconds = refreshTtlMillis / 1000;
    }

    @Override
    public TokenPair issueTokens(UserId userId, String email, Role role) {
        Instant now = Instant.now();
        String access = Jwts.builder()
                .subject(userId.value().toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .signWith(signingKey)
                .compact();

        String refresh = Jwts.builder()
                .subject(userId.value().toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .signWith(signingKey)
                .compact();

        return new TokenPair(access, refresh, accessTtlSeconds);
    }

    @Override
    public TokenPair refresh(String refreshToken) {
        Claims claims = parseClaims(refreshToken);

        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new InvalidCredentialsException("Not a refresh token");
        }

        UserId userId = UserId.of(UUID.fromString(claims.getSubject()));

        String email = claims.get("email", String.class);

        Role role = Role.valueOf(claims.get("role", String.class));

        return issueTokens(userId, email, role);
    }

    public Claims parseClaims(String token) {
        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return jws.getPayload();
        } catch (JwtException e) {
            throw new InvalidCredentialsException("Invalid or expired token");
        }
    }

    public boolean isAccessToken(String token) {
        try {
            return "access".equals(parseClaims(token).get("type", String.class));
        } catch (InvalidCredentialsException e) {
            return false;
        }
    }
}
