package com.company.userservice.infrastructure.persistence.query;

import com.company.userservice.domain.model.UserId;
import com.company.userservice.domain.model.UserReadModel;
import com.company.userservice.domain.port.infrastructure.UserQueryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserQueryRepositoryAdapter implements UserQueryRepository {

    private final UserDocumentRepository documentRepository;

    public UserQueryRepositoryAdapter(UserDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public Optional<UserReadModel> findById(UserId id) {
        return documentRepository.findById(id.value()).map(UserDocumentMapper::toDomain);
    }

    @Override
    public List<UserReadModel> findAll() {
        return documentRepository.findAll().stream().map(UserDocumentMapper::toDomain).toList();
    }

    @Override
    public List<UserReadModel> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }

        return documentRepository.searchByText(query).stream().map(UserDocumentMapper::toDomain).toList();
    }

    @Override
    public UserReadModel save(UserReadModel readModel) {
        UserDocument saved = documentRepository.save(UserDocumentMapper.toDocument(readModel));
        return UserDocumentMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UserId id) {
        documentRepository.deleteById(id.value());
    }
}
