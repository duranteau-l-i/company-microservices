package com.company.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtTokenValidator {

    private final SecretKey signingKey;

    public JwtTokenValidator(
            @Value("${app.security.jwt.secret:local-dev-secret-change-me-to-a-long-random-string-at-least-256-bits-long}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims validateAndExtractClaims(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload();
        } catch (JwtException e) {
            throw new RuntimeException("Invalid or expired token", e);
        }
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }
}
