package com.company.userservice.application.query;

import com.company.userservice.domain.exception.UserNotFoundException;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.in.GetUserUseCase;
import com.company.userservice.domain.port.out.UserQueryRepository;

public class GetUserHandler implements GetUserUseCase {

    private final UserQueryRepository repository;

    public GetUserHandler(UserQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserReadModel get(Query query) {
        return repository.findById(query.targetId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + query.targetId()));
    }
}
