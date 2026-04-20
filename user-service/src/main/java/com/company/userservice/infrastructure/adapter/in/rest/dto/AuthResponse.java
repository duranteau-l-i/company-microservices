package com.company.userservice.infrastructure.adapter.in.rest.dto;

import com.company.userservice.domain.port.out.TokenProvider.TokenPair;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn) {
    public static AuthResponse from(TokenPair pair) {
        return new AuthResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds());
    }
}
