package com.company.apigateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtAuthenticationFilterTest {

    private static final String JWT_SECRET =
            "local-dev-secret-change-me-to-a-long-random-string-at-least-256-bits-long";

    @Autowired
    private WebTestClient webTestClient;

    private String generateAccessToken() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "test@example.com")
                .claim("role", "USER")
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(1800)))
                .signWith(key)
                .compact();
    }

    private String generateRefreshToken() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "test@example.com")
                .claim("role", "USER")
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(604800)))
                .signWith(key)
                .compact();
    }

    @Test
    void publicEndpoint_noToken_isForwarded() {
        // Public endpoint — filter lets it through, gateway attempts to route
        // Route target (localhost:9999) is not running, so we get 503 or connection error,
        // but NOT 401. Any non-401 response means the filter passed the request.
        webTestClient.post()
                .uri("/api/users/signup")
                .exchange()
                .expectStatus().value(status -> {
                    // Anything except 401 is acceptable — the filter let the request through
                    assert status != 401 : "Expected request to pass the filter, but got 401";
                });
    }

    @Test
    void protectedEndpoint_noToken_returns401() {
        webTestClient.get()
                .uri("/api/companies/123")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedEndpoint_invalidToken_returns401() {
        webTestClient.get()
                .uri("/api/companies/123")
                .header("Authorization", "Bearer this.is.not.a.valid.jwt")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedEndpoint_refreshToken_returns401() {
        // Refresh token should be rejected — only access tokens are allowed
        String refreshToken = generateRefreshToken();
        webTestClient.get()
                .uri("/api/companies/123")
                .header("Authorization", "Bearer " + refreshToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedEndpoint_validAccessToken_isForwarded() {
        // Valid access token — filter lets it through, gateway attempts to route to downstream
        // Route target (localhost:9999) is not running, so we get 503 or similar, but NOT 401.
        String accessToken = generateAccessToken();
        webTestClient.get()
                .uri("/api/companies/123")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().value(status -> {
                    assert status != 401 : "Expected request to pass the filter, but got 401";
                });
    }

    @Test
    void protectedEndpoint_missingBearerPrefix_returns401() {
        String accessToken = generateAccessToken();
        webTestClient.get()
                .uri("/api/companies/123")
                .header("Authorization", accessToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void actuatorHealth_noToken_isForwarded() {
        // /actuator/health is a public path
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().value(status -> {
                    assert status != 401 : "Expected actuator/health to be public, but got 401";
                });
    }
}
