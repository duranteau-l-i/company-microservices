package com.company.officerservice.infrastructure.persistence.query;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "known_companies")
public class KnownCompanyDocument {

    @Id
    private UUID id;

    private UUID ownerId;

    public KnownCompanyDocument() {}

    public KnownCompanyDocument(UUID id, UUID ownerId) {
        this.id = id;
        this.ownerId = ownerId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }
}
