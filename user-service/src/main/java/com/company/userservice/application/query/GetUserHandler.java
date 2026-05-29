package com.company.userservice.application.query;

import com.company.userservice.domain.exception.UserNotFoundException;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.usecases.GetUserUseCase;
import com.company.userservice.domain.port.infrastructure.UserQueryRepository;

public class GetUserHandler implements GetUserUseCase {

    private final UserQueryRepository repository;

    public GetUserHandler(UserQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserReadModel get(Query query) {
        boolean isSelf = query.callerId().equals(query.targetId());
        boolean isPrivileged = query.callerRole() == Role.ADMIN || query.callerRole() == Role.MANAGER;
        if (!isSelf && !isPrivileged) {
            throw new UserNotFoundException("User not found: " + query.targetId());
        }
        return repository.findById(query.targetId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + query.targetId()));
    }
}
