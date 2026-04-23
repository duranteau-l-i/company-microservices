CREATE TABLE companies (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    registration_number VARCHAR(100) NOT NULL UNIQUE,
    street              VARCHAR(255) NOT NULL,
    city                VARCHAR(100) NOT NULL,
    postal_code         VARCHAR(20)  NOT NULL,
    country             VARCHAR(100) NOT NULL,
    owner_id            UUID         NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_companies_owner_id ON companies (owner_id);
CREATE INDEX idx_companies_registration_number ON companies (registration_number);

CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
