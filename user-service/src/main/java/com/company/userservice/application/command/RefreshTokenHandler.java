package com.company.userservice.application.command;

import com.company.userservice.domain.port.in.RefreshTokenUseCase;
import com.company.userservice.domain.port.out.TokenProvider;
import com.company.userservice.domain.port.out.TokenProvider.TokenPair;

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
