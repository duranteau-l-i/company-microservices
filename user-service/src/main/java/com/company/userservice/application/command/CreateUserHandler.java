package com.company.userservice.application.command;

import com.company.userservice.domain.exception.DuplicateEmailException;
import com.company.userservice.domain.exception.InsufficientPermissionException;
import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.in.CreateUserUseCase;
import com.company.userservice.domain.port.out.PasswordHasher;
import com.company.userservice.domain.port.out.UserCommandRepository;
import com.company.userservice.domain.port.out.UserEventPublisher;

public class CreateUserHandler implements CreateUserUseCase {

    private final UserCommandRepository repository;
    private final UserEventPublisher publisher;
    private final PasswordHasher passwordHasher;

    public CreateUserHandler(UserCommandRepository repository,
                             UserEventPublisher publisher,
                             PasswordHasher passwordHasher) {
        this.repository = repository;
        this.publisher = publisher;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public UserReadModel create(Command command) {
        if (!command.callerRole().canCreate(command.targetRole())) {
            throw new InsufficientPermissionException(
                    command.callerRole() + " cannot create " + command.targetRole());
        }

        EmailAddress email = EmailAddress.of(command.email());
        if (repository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email already registered: " + email.value());
        }

        String hash = passwordHasher.hash(command.password());

        User.Created created = User.create(email, hash, command.firstName(), command.lastName(), command.targetRole());

        repository.save(created.user());

        publisher.publish(created.event());

        return UserReadModel.from(created.user());
    }
}
