package com.company.officerservice.infrastructure.persistence.query;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "processed_kafka_events")
public class ProcessedEventDocument {

    @Id
    private UUID eventId;
    private Instant processedAt;

    public ProcessedEventDocument() {}

    public ProcessedEventDocument(UUID eventId, Instant processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    public UUID getEventId() { return eventId; }

    public Instant getProcessedAt() { return processedAt; }
}
