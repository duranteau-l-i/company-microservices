package com.company.userservice.infrastructure.adapter.out.persistence.query;

import com.company.userservice.domain.model.EmailAddress;
import com.company.userservice.domain.model.Role;
import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;

public final class UserDocumentMapper {

    private UserDocumentMapper() {}

    public static UserDocument toDocument(UserReadModel model) {
        return new UserDocument(
                model.id().value(),
                model.email().value(),
                model.firstName(),
                model.lastName(),
                model.role().name(),
                model.active(),
                model.createdAt(),
                model.updatedAt());
    }

    public static UserReadModel toDomain(UserDocument doc) {
        return new UserReadModel(
                UserId.of(doc.getId()),
                EmailAddress.of(doc.getEmail()),
                doc.getFirstName(),
                doc.getLastName(),
                Role.valueOf(doc.getRole()),
                doc.isActive(),
                doc.getCreatedAt(),
                doc.getUpdatedAt());
    }
}
