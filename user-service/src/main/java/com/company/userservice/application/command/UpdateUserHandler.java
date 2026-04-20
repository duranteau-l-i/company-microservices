package com.company.userservice.application.command;

import com.company.userservice.domain.event.UserUpdatedEvent;
import com.company.userservice.domain.exception.InsufficientPermissionException;
import com.company.userservice.domain.exception.UserNotFoundException;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.in.UpdateUserUseCase;
import com.company.userservice.domain.port.out.UserCommandRepository;
import com.company.userservice.domain.port.out.UserEventPublisher;

public class UpdateUserHandler implements UpdateUserUseCase {

    private final UserCommandRepository repository;
    private final UserEventPublisher publisher;

    public UpdateUserHandler(UserCommandRepository repository, UserEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Override
    public UserReadModel update(Command command) {
        UserId targetId = command.targetId();
        boolean self = command.callerId().equals(targetId);
        boolean privileged = command.callerRole() == Role.ADMIN || command.callerRole() == Role.MANAGER;
        if (!self && !privileged) {
            throw new InsufficientPermissionException("Cannot update another user");
        }
        User user = repository.findById(targetId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + targetId));
        user.updateProfile(command.firstName(), command.lastName());
        repository.save(user);
        publisher.publish(UserUpdatedEvent.of(user.id(), user.email(), user.firstName(), user.lastName(), user.role(), user.updatedAt()));
        return UserReadModel.from(user);
    }
}
