package com.company.userservice.domain.port.in;

import com.company.userservice.domain.port.out.TokenProvider.TokenPair;

public interface SignInUseCase {
    TokenPair signIn(Command command);

    record Command(String email, String password) {}
}
