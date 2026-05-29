package com.company.userservice.security;

import com.company.userservice.domain.exception.InvalidCredentialsException;
import com.company.userservice.domain.model.RefreshToken;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.port.infrastructure.RefreshTokenRepository;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider implements TokenProvider {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey signingKey;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final RefreshTokenRepository refreshTokenRepository;

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.access-token-expiration:1800000}") long accessTtlMillis,
            @Value("${app.security.jwt.refresh-token-expiration:604800000}") long refreshTtlMillis,
            RefreshTokenRepository refreshTokenRepository) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.security.jwt.secret must be at least 32 characters");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlMillis / 1000;
        this.refreshTtlSeconds = refreshTtlMillis / 1000;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public TokenPair issueTokens(UserId userId, String email, Role role) {
        String accessJwt = buildAccessJwt(userId, email, role);
        String rawRefresh = generateOpaqueToken();
        refreshTokenRepository.save(new RefreshToken(
                userId, email, role,
                sha256(rawRefresh),
                Instant.now().plusSeconds(refreshTtlSeconds)
        ));
        return new TokenPair(accessJwt, rawRefresh, accessTtlSeconds);
    }

    @Override
    public TokenPair refresh(String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken stored = refreshTokenRepository.findByHash(hash)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));

        if (stored.isExpired()) {
            refreshTokenRepository.deleteByHash(hash);
            throw new InvalidCredentialsException("Refresh token expired");
        }

        refreshTokenRepository.deleteByHash(hash);
        return issueTokens(stored.userId(), stored.email(), stored.role());
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

    private String buildAccessJwt(UserId userId, String email, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.value().toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .signWith(signingKey)
                .compact();
    }

    private static String generateOpaqueToken() {
        byte[] raw = new byte[32];
        SECURE_RANDOM.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
