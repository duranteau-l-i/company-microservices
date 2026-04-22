package com.company.userservice.presentation.kafka;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends MongoRepository<ProcessedEventDocument, UUID> {
}
