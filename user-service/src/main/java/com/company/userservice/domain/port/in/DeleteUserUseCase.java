package com.company.userservice.domain.port.in;

import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;

public interface DeleteUserUseCase {
    void delete(Command command);

    record Command(UserId callerId, Role callerRole, UserId targetId) {}
}
