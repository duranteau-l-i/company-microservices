package com.company.userservice.infrastructure.persistence.command;

import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.port.infrastructure.UserCommandRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class UserCommandRepositoryAdapter implements UserCommandRepository {

    private final UserEntityRepository jpa;

    public UserCommandRepositoryAdapter(UserEntityRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity saved = jpa.save(UserEntityMapper.toEntity(user));
        return UserEntityMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId id) {
        return jpa.findById(id.value()).map(UserEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(EmailAddress email) {
        return jpa.findByEmail(email.value()).map(UserEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(EmailAddress email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    @Transactional
    public void delete(UserId id) {
        jpa.deleteById(id.value());
    }
}
