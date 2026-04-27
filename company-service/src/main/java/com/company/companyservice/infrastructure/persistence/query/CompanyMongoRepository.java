package com.company.companyservice.infrastructure.persistence.query;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface CompanyMongoRepository extends MongoRepository<CompanyDocument, UUID> {

    List<CompanyDocument> findByOwnerId(UUID ownerId);

    List<CompanyDocument> findByNameContainingIgnoreCase(String name);

    List<CompanyDocument> findByOfficers_OfficerId(UUID officerId);
}
