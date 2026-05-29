package com.company.userservice.application.command;

import com.company.userservice.domain.event.UserDeletedEvent;
import com.company.userservice.domain.exception.InsufficientPermissionException;
import com.company.userservice.domain.exception.UserNotFoundException;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.port.usecases.DeleteUserUseCase;
import com.company.userservice.domain.port.infrastructure.RefreshTokenRepository;
import com.company.userservice.domain.port.infrastructure.UserCommandRepository;
import com.company.userservice.domain.port.infrastructure.UserEventPublisher;

public class DeleteUserHandler implements DeleteUserUseCase {

    private final UserCommandRepository repository;
    private final UserEventPublisher publisher;
    private final RefreshTokenRepository refreshTokenRepository;

    public DeleteUserHandler(UserCommandRepository repository, UserEventPublisher publisher,
                             RefreshTokenRepository refreshTokenRepository) {
        this.repository = repository;
        this.publisher = publisher;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void delete(Command command) {
        if (command.callerRole() != Role.ADMIN) {
            throw new InsufficientPermissionException("Only ADMIN can delete users");
        }

        User user = repository.findById(command.targetId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + command.targetId()));

        user.deactivate();

        repository.save(user);
        refreshTokenRepository.deleteAllByUserId(user.id());

        publisher.publish(UserDeletedEvent.of(user.id(), user.updatedAt()));
    }
}
