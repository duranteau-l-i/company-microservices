package com.company.userservice.domain.port.usecases;

import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;

public interface GetUserUseCase {
    UserReadModel get(Query query);

    record Query(UserId callerId, Role callerRole, UserId targetId) {}
}
