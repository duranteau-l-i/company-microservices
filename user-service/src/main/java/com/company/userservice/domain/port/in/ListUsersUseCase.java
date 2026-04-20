package com.company.userservice.domain.port.in;

import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;

import java.util.List;

public interface ListUsersUseCase {
    List<UserReadModel> list(Query query);

    record Query(UserId callerId, Role callerRole, String search) {}
}
