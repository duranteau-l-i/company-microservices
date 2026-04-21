package com.company.userservice.application.command;

import com.company.userservice.domain.exception.DuplicateEmailException;
import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.usecases.SignUpUseCase;
import com.company.userservice.domain.port.infrastructure.PasswordHasher;
import com.company.userservice.domain.port.infrastructure.UserCommandRepository;
import com.company.userservice.domain.port.infrastructure.UserEventPublisher;

public class SignUpHandler implements SignUpUseCase {

    private final UserCommandRepository repository;
    private final UserEventPublisher publisher;
    private final PasswordHasher passwordHasher;

    public SignUpHandler(UserCommandRepository repository,
                         UserEventPublisher publisher,
                         PasswordHasher passwordHasher) {
        this.repository = repository;
        this.publisher = publisher;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public UserReadModel signUp(Command command) {
        EmailAddress email = EmailAddress.of(command.email());

        if (repository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email already registered: " + email.value());
        }

        String hash = passwordHasher.hash(command.password());

        User.Created created = User.create(email, hash, command.firstName(), command.lastName(), Role.USER);

        repository.save(created.user());

        publisher.publish(created.event());

        return UserReadModel.from(created.user());
    }
}
