package com.company.userservice.domain.port.in;

import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;

public interface UpdateUserUseCase {
    UserReadModel update(Command command);

    record Command(
            UserId callerId,
            Role callerRole,
            UserId targetId,
            String firstName,
            String lastName) {}
}
