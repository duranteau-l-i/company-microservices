package com.company.userservice.domain.port.usecases;

import com.company.userservice.domain.port.infrastructure.TokenProvider.TokenPair;

public interface RefreshTokenUseCase {
    TokenPair refresh(Command command);

    record Command(String refreshToken) {}
}
