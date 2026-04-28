package com.company.companyservice.infrastructure.persistence.query;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface ProcessedEventDocumentRepository extends MongoRepository<ProcessedEventDocument, UUID> {
}
