package com.company.officerservice.integration.controller;

import com.company.officerservice.domain.model.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

class TestJwtHelper {

    static final String TEST_SECRET =
            "integration-test-secret-that-is-at-least-64-bytes-long-xxxxxxxxxxxxxxxxx";

    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

    static String accessToken(UUID userId, String email, Role role) {
        Instant now = Instant.now();
        String jwt = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(1800)))
                .signWith(KEY)
                .compact();
        return "Bearer " + jwt;
    }
}
