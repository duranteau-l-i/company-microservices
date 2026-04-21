package com.company.userservice.application.command;

import com.company.userservice.domain.exception.InvalidCredentialsException;
import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.port.usecases.SignInUseCase;
import com.company.userservice.domain.port.infrastructure.PasswordHasher;
import com.company.userservice.domain.port.infrastructure.TokenProvider;
import com.company.userservice.domain.port.infrastructure.TokenProvider.TokenPair;
import com.company.userservice.domain.port.infrastructure.UserCommandRepository;

public class SignInHandler implements SignInUseCase {

    private final UserCommandRepository repository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;

    public SignInHandler(UserCommandRepository repository,
                         PasswordHasher passwordHasher,
                         TokenProvider tokenProvider) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public TokenPair signIn(Command command) {
        EmailAddress email;
        try {
            email = EmailAddress.of(command.email());
        } catch (IllegalArgumentException ex) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        User user = repository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!user.active()) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return tokenProvider.issueTokens(user.id(), user.email().value(), user.role());
    }
}
