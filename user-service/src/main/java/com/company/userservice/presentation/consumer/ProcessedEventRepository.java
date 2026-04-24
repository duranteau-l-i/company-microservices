package com.company.userservice.presentation.consumer;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends MongoRepository<ProcessedEventDocument, UUID> {
}
