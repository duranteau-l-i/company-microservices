package com.company.userservice.infrastructure.adapter.in.kafka;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends MongoRepository<ProcessedEventDocument, UUID> {
}
