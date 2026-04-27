package com.company.officerservice.infrastructure.persistence.query;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "known_companies")
public class KnownCompanyDocument {

    @Id
    private UUID id;

    public KnownCompanyDocument() {}

    public KnownCompanyDocument(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
