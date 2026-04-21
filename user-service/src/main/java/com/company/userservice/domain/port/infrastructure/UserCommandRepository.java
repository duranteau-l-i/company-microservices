package com.company.userservice.domain.port.infrastructure;

import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.model.UserId;

import java.util.Optional;

public interface UserCommandRepository {
    User save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(EmailAddress email);

    boolean existsByEmail(EmailAddress email);

    void delete(UserId id);
}
