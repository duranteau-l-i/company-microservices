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
public class CompanyQueryRepositoryAdapter implements CompanyQueryRepository {

    private final CompanyDocumentRepository documentRepository;

    public CompanyQueryRepositoryAdapter(CompanyDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public Optional<CompanyFullView> findFullById(CompanyId id) {
        return documentRepository.findById(id.value()).map(CompanyDocumentMapper::toFullView);
    }

    @Override
    public Optional<CompanyRestrictedView> findRestrictedById(CompanyId id) {
        return documentRepository.findById(id.value()).map(CompanyDocumentMapper::toRestrictedView);
    }

    @Override
    public List<CompanyFullView> findAllFull() {
        return documentRepository.findAll().stream().map(CompanyDocumentMapper::toFullView).toList();
    }

    @Override
    public List<CompanyFullView> findFullByOwnerId(UUID ownerId) {
        return documentRepository.findByOwnerId(ownerId).stream().map(CompanyDocumentMapper::toFullView).toList();
    }

    @Override
    public List<CompanyRestrictedView> search(String query) {
        if (query == null || query.isBlank()) {
            return documentRepository.findAll().stream().map(CompanyDocumentMapper::toRestrictedView).toList();
        }
        return documentRepository.findByNameContainingIgnoreCase(query).stream()
                .map(CompanyDocumentMapper::toRestrictedView).toList();
    }

    @Override
    public CompanyFullView save(CompanyFullView view) {
        CompanyDocument saved = documentRepository.save(CompanyDocumentMapper.toDocument(view));
        return CompanyDocumentMapper.toFullView(saved);
    }

    @Override
    public void deleteById(CompanyId id) {
        documentRepository.deleteById(id.value());
    }

    @Override
    public List<CompanyFullView> findCompaniesContainingOfficer(UUID officerId) {
        return documentRepository.findByOfficers_OfficerId(officerId).stream()
                .map(CompanyDocumentMapper::toFullView)
                .toList();
    }
}
