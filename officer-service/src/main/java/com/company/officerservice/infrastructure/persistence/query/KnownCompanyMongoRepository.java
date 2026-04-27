package com.company.officerservice.infrastructure.persistence.query;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface KnownCompanyMongoRepository extends MongoRepository<KnownCompanyDocument, UUID> {
}
