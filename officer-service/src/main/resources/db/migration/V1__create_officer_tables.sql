CREATE TABLE officers (
    id           UUID         PRIMARY KEY,
    first_name   VARCHAR(100) NOT NULL,
    last_name    VARCHAR(100) NOT NULL,
    date_of_birth DATE        NOT NULL,
    nationality  VARCHAR(100) NOT NULL,
    street       VARCHAR(255) NOT NULL,
    city         VARCHAR(100) NOT NULL,
    postal_code  VARCHAR(20)  NOT NULL,
    country      VARCHAR(100) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    phone        VARCHAR(50),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_officers_last_name ON officers (last_name);
CREATE INDEX idx_officers_name_dob  ON officers (first_name, last_name, date_of_birth);

CREATE TABLE company_links (
    id               UUID        PRIMARY KEY,
    officer_id       UUID        NOT NULL REFERENCES officers (id) ON DELETE CASCADE,
    company_id       UUID        NOT NULL,
    title            VARCHAR(255) NOT NULL,
    appointment_date DATE        NOT NULL,
    resignation_date DATE,
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_officer_company_title UNIQUE (officer_id, company_id, title)
);

CREATE INDEX idx_company_links_officer_id  ON company_links (officer_id);
CREATE INDEX idx_company_links_company_id  ON company_links (company_id);

CREATE TABLE processed_events (
    event_id     UUID        PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
