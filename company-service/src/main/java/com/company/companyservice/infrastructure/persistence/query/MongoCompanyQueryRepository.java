package com.company.companyservice.infrastructure.persistence.query;

import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.port.infrastructure.CompanyQueryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MongoCompanyQueryRepository implements CompanyQueryRepository {

    private final CompanyMongoRepository mongo;

    public MongoCompanyQueryRepository(CompanyMongoRepository mongo) {
        this.mongo = mongo;
    }

    @Override
    public Optional<CompanyFullView> findFullById(CompanyId id) {
        return mongo.findById(id.value()).map(CompanyDocumentMapper::toFullView);
    }

    @Override
    public Optional<CompanyRestrictedView> findRestrictedById(CompanyId id) {
        return mongo.findById(id.value()).map(CompanyDocumentMapper::toRestrictedView);
    }

    @Override
    public List<CompanyFullView> findAllFull() {
        return mongo.findAll().stream().map(CompanyDocumentMapper::toFullView).toList();
    }

    @Override
    public List<CompanyFullView> findFullByOwnerId(UUID ownerId) {
        return mongo.findByOwnerId(ownerId).stream().map(CompanyDocumentMapper::toFullView).toList();
    }

    @Override
    public List<CompanyFullView> findAllByOfficerId(UUID officerId) {
        return mongo.findByOfficers_OfficerId(officerId).stream().map(CompanyDocumentMapper::toFullView).toList();
    }

    @Override
    public List<CompanyRestrictedView> search(String query) {
        if (query == null || query.isBlank()) {
            return mongo.findAll().stream().map(CompanyDocumentMapper::toRestrictedView).toList();
        }
        return mongo.findByNameContainingIgnoreCase(query).stream()
                .map(CompanyDocumentMapper::toRestrictedView).toList();
    }

    @Override
    public CompanyFullView save(CompanyFullView view) {
        CompanyDocument saved = mongo.save(CompanyDocumentMapper.toDocument(view));
        return CompanyDocumentMapper.toFullView(saved);
    }

    @Override
    public void deleteById(CompanyId id) {
        mongo.deleteById(id.value());
    }
}
