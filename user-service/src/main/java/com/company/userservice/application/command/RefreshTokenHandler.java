package com.company.userservice.application.command;

import com.company.userservice.domain.port.usecases.RefreshTokenUseCase;
import com.company.userservice.domain.port.infrastructure.TokenProvider;
import com.company.userservice.domain.port.infrastructure.TokenProvider.TokenPair;

public class RefreshTokenHandler implements RefreshTokenUseCase {

    private final TokenProvider tokenProvider;

    public RefreshTokenHandler(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public TokenPair refresh(Command command) {
        return tokenProvider.refresh(command.refreshToken());
    }
}
