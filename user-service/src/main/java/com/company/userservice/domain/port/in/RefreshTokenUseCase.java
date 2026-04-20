package com.company.userservice.domain.port.in;

import com.company.userservice.domain.port.out.TokenProvider.TokenPair;

public interface RefreshTokenUseCase {
    TokenPair refresh(Command command);

    record Command(String refreshToken) {}
}
