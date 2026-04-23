package com.company.userservice.infrastructure.persistence.query;

import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.infrastructure.UserQueryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoUserQueryRepository implements UserQueryRepository {

    private final UserMongoRepository mongo;

    public MongoUserQueryRepository(UserMongoRepository mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<UserReadModel> findById(UserId id) {
        return mongo.findById(id.value()).map(UserDocumentMapper::toDomain);
    }

    @Override
    public List<UserReadModel> findAll() {
        return mongo.findAll().stream().map(UserDocumentMapper::toDomain).toList();
    }

    @Override
    public List<UserReadModel> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }

        return mongo.searchByText(query).stream().map(UserDocumentMapper::toDomain).toList();
    }

    @Override
    public UserReadModel save(UserReadModel readModel) {
        UserDocument saved = mongo.save(UserDocumentMapper.toDocument(readModel));
        return UserDocumentMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UserId id) {
        mongo.deleteById(id.value());
    }
}
