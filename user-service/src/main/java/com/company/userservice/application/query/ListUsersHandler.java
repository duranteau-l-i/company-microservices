package com.company.userservice.application.query;

import com.company.userservice.domain.exception.InsufficientPermissionException;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.in.ListUsersUseCase;
import com.company.userservice.domain.port.out.UserQueryRepository;

import java.util.List;

public class ListUsersHandler implements ListUsersUseCase {

    private final UserQueryRepository repository;

    public ListUsersHandler(UserQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UserReadModel> list(Query query) {
        if (query.callerRole() == Role.USER) {
            throw new InsufficientPermissionException("USER role cannot list users");
        }

        if (query.search() != null && !query.search().isBlank()) {
            return repository.search(query.search());
        }

        return repository.findAll();
    }
}
