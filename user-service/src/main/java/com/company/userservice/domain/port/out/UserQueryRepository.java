package com.company.userservice.domain.port.out;

import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;

import java.util.List;
import java.util.Optional;

public interface UserQueryRepository {
    Optional<UserReadModel> findById(UserId id);

    List<UserReadModel> findAll();

    List<UserReadModel> search(String query);

    UserReadModel save(UserReadModel readModel);

    void deleteById(UserId id);
}
