package com.company.userservice.domain.port.in;

import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;

public interface CreateUserUseCase {
    UserReadModel create(Command command);

    record Command(
            UserId callerId,
            Role callerRole,
            String email,
            String password,
            String firstName,
            String lastName,
            Role targetRole) {}
}
