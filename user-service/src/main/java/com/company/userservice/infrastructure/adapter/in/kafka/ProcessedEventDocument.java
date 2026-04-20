package com.company.userservice.infrastructure.adapter.in.kafka;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "processed_events")
public class ProcessedEventDocument {

    @Id
    private UUID eventId;
    private String eventType;
    private Instant processedAt;

    public ProcessedEventDocument() {}

    public ProcessedEventDocument(UUID eventId, String eventType, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }

    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Instant getProcessedAt() { return processedAt; }
}
