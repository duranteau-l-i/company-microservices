package com.company.userservice.domain.port.usecases;

import com.company.userservice.domain.port.infrastructure.TokenProvider.TokenPair;

public interface SignInUseCase {
    TokenPair signIn(Command command);

    record Command(String email, String password) {}
}
