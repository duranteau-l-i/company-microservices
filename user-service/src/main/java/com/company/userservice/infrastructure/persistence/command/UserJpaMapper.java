package com.company.userservice.infrastructure.persistence.command;

import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.User;
import com.company.userservice.domain.model.UserId;

public final class UserJpaMapper {

    private UserJpaMapper() {}

    public static UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.id().value(),
                user.email().value(),
                user.passwordHash(),
                user.firstName(),
                user.lastName(),
                user.role().name(),
                user.active(),
                user.createdAt(),
                user.updatedAt());
    }

    public static User toDomain(UserJpaEntity entity) {
        return new User(
                UserId.of(entity.getId()),
                EmailAddress.of(entity.getEmail()),
                entity.getPasswordHash(),
                entity.getFirstName(),
                entity.getLastName(),
                Role.valueOf(entity.getRole()),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
