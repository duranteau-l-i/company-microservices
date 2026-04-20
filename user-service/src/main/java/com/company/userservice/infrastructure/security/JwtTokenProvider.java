package com.company.userservice.infrastructure.security;

import com.company.userservice.domain.exception.InvalidCredentialsException;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.port.out.TokenProvider;
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
            @Value("${app.jwt.secret:change-me-change-me-change-me-change-me-64}") String secret,
            @Value("${app.jwt.access-ttl-seconds:1800}") long accessTtlSeconds,
            @Value("${app.jwt.refresh-ttl-seconds:604800}") long refreshTtlSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
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
