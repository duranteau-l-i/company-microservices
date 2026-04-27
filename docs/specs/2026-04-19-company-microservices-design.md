# Company Microservices Platform — Design Specification

## Overview

A microservices platform for managing companies and their officers, built with Java 21, Spring Boot, and the Spring Cloud ecosystem. The system follows DDD and CQRS principles with PostgreSQL as the write store and MongoDB as the read store.

## Repository Structure

```
company-microservices/
├── config-service/           # Spring Cloud Config Server
├── registry-service/         # Eureka Service Registry
├── api-gateway/              # Spring Cloud Gateway
├── user-service/             # User bounded context
├── company-service/          # Company bounded context
├── officer-service/          # Officer bounded context
├── config-repo/              # Git-backed config files
│   ├── user-service.yml
│   ├── company-service.yml
│   ├── officer-service.yml
│   ├── api-gateway.yml
│   └── application.yml       # shared defaults
├── docker-compose.yml         # Full stack
├── docker-compose.infra.yml   # Infrastructure only
└── .env
```

Each service has a fully independent `pom.xml` with `spring-boot-starter-parent` as parent. No shared parent POM, no shared library — maximum isolation.

## Infrastructure Services

### Config Service
- Spring Cloud Config Server
- Serves configuration from `config-repo/` (local git-backed)
- Profiles: `dev`, `docker`, `test`

### Registry Service
- Eureka Server
- All services register on startup, discover each other through it
- Services register for discovery; no service-to-service HTTP calls use Eureka at runtime (all inter-service communication is event-driven)

### API Gateway
- Spring Cloud Gateway
- Single entry point for all external requests
- Validates JWT signature and expiration (not authorization)
- Routes by path prefix: `/api/users/**`, `/api/companies/**`, `/api/officers/**`
- Handles CORS

---

## Service Architecture (DDD / Hexagonal / CQRS)

Each business service follows this internal structure:

```
<service>/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/java/com/company/<service>/
    │   ├── domain/
    │   │   ├── model/          # Aggregates, Entities, Value Objects
    │   │   ├── event/          # Domain events
    │   │   ├── exception/      # Domain exceptions
    │   │   └── port/
    │   │       ├── usecases/   # Driving ports (use cases)
    │   │       └── infrastructure/ # Driven ports (repository, messaging)
    │   ├── application/
    │   │   ├── command/        # Write side — command handlers
    │   │   └── query/          # Read side — query handlers
    │   ├── infrastructure/
    │   │   ├── messaging/        # Kafka producers
    │   │   ├── persistence/
    │   │   │   ├── command/      # PostgreSQL (JPA)
    │   │   │   └── query/        # MongoDB
    │   │   └── (no feign — all inter-service communication is event-driven)
    │   ├── presentation/
    │   │   ├── rest/             # Controllers
    │   │   └── kafka/            # Kafka consumers
    │   ├── config/
    │   └── security/
    │   └── shared/             # DTOs, mappers (service-local)
    ├── main/resources/
    │   ├── application.yml
    │   ├── application-dev.yml
    │   ├── application-docker.yml
    │   ├── application-test.yml
    │   ├── db/migration/       # Flyway (PostgreSQL)
    │   └── bootstrap.yml       # Config Server connection
    └── test/java/com/company/<service>/
        ├── unit/
        │   ├── domain/
        │   └── application/
        └── stubs/              # InMemory stub implementations
        ├── integration/        # Testcontainers
        └── e2e/                # Full service HTTP tests
```

**Key rules:**
- Domain layer has zero framework imports — pure Java
- Ports define contracts: `usecases/` = use case interfaces, `infrastructure/` = repository/messaging interfaces
- Commands write to PostgreSQL, queries read from MongoDB
- CQRS sync: command persisted → domain event to Kafka → internal consumer updates MongoDB read model

---

## User Service

### Domain Model
- **User (Aggregate Root)** — id, email, password (hashed), firstName, lastName, role, createdAt, updatedAt, active
- **Role (Value Object)** — enum: ADMIN, MANAGER, USER

### Auth Flow
- `POST /api/users/signup` — open, creates USER account, returns JWT
- `POST /api/users/signin` — validates credentials, returns access + refresh token
- `POST /api/users/auth/refresh` — rotates refresh token, issues new access token

### JWT
- Payload: userId, email, role, expiration
- Signed with shared secret from Config Service
- Access token: 30 min, refresh token: 7 days (stored hashed in PostgreSQL)

### Authorization Matrix

| Action | ADMIN | MANAGER | USER |
|---|---|---|---|
| Create ADMIN | No (seeded only) | No | No |
| Create MANAGER | Yes | No | No |
| Create USER | Yes | Yes | No |
| Signup (self) | — | — | Open |
| View all users | Yes | Yes | No |
| View own profile | Yes | Yes | Yes |
| Update any user | Yes | No | No |
| Update own profile | Yes | Yes | Yes |
| Delete user | Yes | No | No |

### Default Admin
On first startup, if no ADMIN exists, one is created from environment variables. Only way to create an ADMIN.

### Events Published
- `UserCreatedEvent`, `UserUpdatedEvent`, `UserDeletedEvent`

---

## Company Service

### Domain Model
- **Company (Aggregate Root)** — id, name, registrationNumber, address, ownerId, status (ACTIVE/INACTIVE), createdAt, updatedAt
- **Address (Value Object)** — street, city, postalCode, country
- **OfficerSummary (Value Object)** — read-only reference in MongoDB read model, synced via Kafka

### REST API
- `POST /api/companies` — create
- `GET /api/companies/{id}` — get with officers
- `PUT /api/companies/{id}` — update
- `DELETE /api/companies/{id}` — delete
- `GET /api/companies` — list (filtered by role)
- `GET /api/companies/search` — public restricted view

### Authorization Matrix

| Action | ADMIN | MANAGER | USER (owner) | USER (non-owner) |
|---|---|---|---|---|
| Create | Yes | No | Yes | Yes |
| View own (full + officers) | Yes | Yes | Yes | No |
| View other (restricted) | Yes | Yes | No | Yes |
| Update own | Yes | Yes | Yes | No |
| Update other | Yes | Yes | No | No |
| Delete own | Yes | No | Yes | No |
| Delete other | Yes | No | No | No |
| List all | Yes | Yes | No | No |
| List own | Yes | Yes | Yes | Yes |

### Restricted View
name, registrationNumber, owner display name, status. No address, no officers.

### Officer Read Model
Officers are embedded directly in the `CompanyFullView` MongoDB document. The read model is kept in sync by `OfficerEventConsumer` handling `OfficerLinkedToCompanyEvent`, `OfficerUnlinkedFromCompanyEvent`, `OfficerUpdatedEvent`, and `OfficerDeletedEvent`. No HTTP call to officer-service is made at query time.

### Events
- Published: `CompanyCreatedEvent`, `CompanyUpdatedEvent`, `CompanyDeletedEvent`
- Consumed: `OfficerLinkedToCompanyEvent`, `OfficerUnlinkedFromCompanyEvent`, `OfficerUpdatedEvent`, `OfficerDeletedEvent`

---

## Officer Service

### Domain Model
- **Officer (Aggregate Root)** — id, firstName, lastName, dateOfBirth, nationality, address, email, phone, createdAt, updatedAt
- **CompanyLink (Entity)** — companyId, role/title, appointmentDate, resignationDate, active
- **Address (Value Object)** — street, city, postalCode, country

### Deduplication
Search by `firstName + lastName + dateOfBirth`. If match found, user is prompted to confirm: link existing or create new.

### REST API
- `POST /api/officers` — create
- `GET /api/officers/{id}` — get details
- `PUT /api/officers/{id}` — update
- `DELETE /api/officers/{id}` — delete
- `GET /api/officers/search?firstName=&lastName=&dateOfBirth=` — search for linking
- `GET /api/officers/{id}/companies` — list companies for officer
- `GET /api/officers/company/{companyId}` — list officers for company
- `POST /api/officers/{id}/link` — link to company
- `POST /api/officers/{id}/unlink` — unlink from company

### Authorization Matrix

| Action | ADMIN | MANAGER | USER (owner) | USER (non-owner) |
|---|---|---|---|---|
| Create | Yes | Yes | Yes | No |
| View (own company) | Yes | Yes | Yes | No |
| View (other — restricted) | Yes | Yes | No | Yes |
| Search | Yes | Yes | Yes | No |
| Update | Yes | Yes | No | No |
| Delete | Yes | No | No | No |
| Link to own company | Yes | Yes | Yes | No |
| Unlink from own company | Yes | Yes | Yes | No |
| Link/unlink any | Yes | Yes | No | No |

### Restricted View
firstName, lastName, title/role. No dateOfBirth, address, email, phone.

### Events
- Published: `OfficerCreatedEvent`, `OfficerUpdatedEvent`, `OfficerDeletedEvent`, `OfficerLinkedToCompanyEvent`, `OfficerUnlinkedFromCompanyEvent`
- Consumed: `CompanyDeletedEvent` (deactivates all links for that company)

### Company Existence Validation
- officer-service validates companyId against its own `known_companies` MongoDB projection, maintained by `CompanyEventConsumer` handling `CompanyCreatedEvent` and `CompanyDeletedEvent`
- No HTTP call to company-service is made at link time
- **Eventual consistency note:** if a client links an officer to a company immediately after creating it, the `known_companies` projection may not yet contain the new id — the link fails with 404 `CompanyNotFoundException`; the client should retry

---

## Kafka Event Architecture

### Topics

| Topic | Producer | Consumers |
|---|---|---|
| `user-events` | user-service | company-service (cache owner name) |
| `company-events` | company-service | officer-service (company deletion) |
| `officer-events` | officer-service | company-service (read model sync) |

### Event Envelope

```json
{
  "eventId": "uuid",
  "eventType": "OfficerLinkedToCompanyEvent",
  "aggregateId": "uuid",
  "aggregateType": "Officer",
  "timestamp": "ISO-8601",
  "version": 1,
  "payload": { }
}
```

Each service defines its own local DTO for payloads — no shared library.

### CQRS Sync Flow
1. Command received
2. Domain logic executes, aggregate modified
3. Persisted to PostgreSQL (source of truth)
4. Domain event published to Kafka
5. Internal consumer updates MongoDB read model
6. External consumers react as needed

### Consumer Groups
- One group per service (e.g., `company-service-group`)
- Partitioning by `aggregateId` for ordering per aggregate

### Idempotency
`processed_events` table in PostgreSQL per service. Consumers track processed `eventId`.

### Event Versioning
`version` field in envelope. Forward-compatible (ignore unknown fields), backward-compatible (defaults for missing optional fields).

---

## Docker Infrastructure

### docker-compose.infra.yml

| Container | Image | Port |
|---|---|---|
| postgres | postgres:16 | 5432 |
| mongodb | mongo:7 | 27017 |
| zookeeper | confluentinc/cp-zookeeper | 2181 |
| kafka | confluentinc/cp-kafka | 9092 |
| config-service | custom build | 8888 |
| registry-service | custom build | 8761 |

**PostgreSQL databases:** `user_db`, `company_db`, `officer_db`
**MongoDB databases:** `user_query_db`, `company_query_db`, `officer_query_db`

### docker-compose.yml (full stack)

Adds:

| Container | Port | Depends on |
|---|---|---|
| api-gateway | 8080 | config-service, registry-service |
| user-service | 8081 | config-service, registry-service, postgres, mongodb, kafka |
| company-service | 8082 | config-service, registry-service, postgres, mongodb, kafka |
| officer-service | 8083 | config-service, registry-service, postgres, mongodb, kafka |

### Startup Order
1. postgres, mongodb, zookeeper → kafka
2. config-service (must be ready before others fetch config)
3. registry-service
4. api-gateway + business services

Managed via `depends_on` with `condition: service_healthy`.

### Dockerfile (multi-stage per service)
- Stage 1: Maven build (`mvn clean package -DskipTests`)
- Stage 2: `eclipse-temurin:21-jre-alpine`, copy jar, expose port

### Environment Variables (.env)
Database credentials, JWT secret, Kafka bootstrap servers, Config/Eureka URIs, default admin credentials.

---

## Security Architecture

### JWT Flow
1. Authenticate via `POST /api/users/signin`
2. Receive `{ accessToken, refreshToken, expiresIn }`
3. Send `Authorization: Bearer <accessToken>` on all requests
4. Gateway validates signature + expiration → 401 if invalid
5. Service extracts userId + role → enforces authorization

### Shared Secret
Stored in Config Service, fetched by all services on startup.

### Spring Security per Service
- `JwtAuthenticationFilter` — extracts/validates token, sets `SecurityContext`
- `@PreAuthorize` on use case handlers/controllers
- Role hierarchy: ADMIN > MANAGER > USER
- Public endpoints: signup, signin, refresh, actuator health

### Two-Layer Authorization

| Layer | Checks | Where |
|---|---|---|
| Gateway | Token valid (signature, expiration) | Gateway filter |
| Service | Role permission + ownership | Use case layer |

Ownership checks require domain context — handled in application layer, not security filter.

### Refresh Token
Stored hashed in PostgreSQL (user-service). 7-day expiry. One-time use with rotation.

---

## Inter-Service Communication

All inter-service communication is **event-driven via Kafka**. There are no Feign/HTTP calls between business services.

### How Each Service Avoids Calling Others

**company-service — officer data:**
The `GET /api/companies/{id}` response includes embedded officer summaries read directly from the `CompanyFullView` MongoDB document. `OfficerEventConsumer` keeps this list current by handling `OfficerLinkedToCompanyEvent`, `OfficerUnlinkedFromCompanyEvent`, `OfficerUpdatedEvent`, and `OfficerDeletedEvent`.

**officer-service — company existence:**
Before linking an officer to a company, officer-service checks its local `known_companies` MongoDB projection (maintained by `CompanyEventConsumer` handling `CompanyCreatedEvent` and `CompanyDeletedEvent`). If the company id is not found, a 404 `CompanyNotFoundException` is returned. This is eventually consistent — a brand-new company may not yet appear in the projection; callers should retry on 404.

### Asynchronous (Kafka)
For eventual consistency: read model sync, cross-service state reactions.

**No synchronous chains.** A request never triggers A → B → C synchronously. Multi-service workflows use Kafka.

### Health & Monitoring
- Actuator: `/actuator/health`, `/actuator/info`, `/actuator/metrics`
- Eureka health checks
- Gateway routes only to healthy instances

---

## Testing Strategy

### Unit Tests
- **Scope:** domain logic + use case handlers
- **Pattern:** InMemory stubs (`HashMap`-backed) implementing `infrastructure/` ports
- **Framework:** JUnit 5 + AssertJ
- **No Spring context** — pure Java, fast

### Integration Tests
- **Scope:** adapters (persistence, messaging, REST)
- **Infrastructure:** Testcontainers (PostgreSQL, MongoDB, Kafka)
- **Framework:** JUnit 5 + Spring Boot Test + Testcontainers
- **Includes:** JPA repos, MongoDB repos, Kafka ser/de, controllers (MockMvc)

### End-to-End Tests
- **Scope:** full service behavior through HTTP
- **Infrastructure:** Docker Compose
- **Framework:** JUnit 5 + RestAssured or WebTestClient
- **Includes:** complete user flows, cross-service scenarios, authorization matrix, Kafka propagation

### Test Profiles
- Unit: no Spring, no profile
- Integration: `test` profile, Testcontainers
- E2E: `docker` profile, full stack

---

## Technology Summary

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Build | Maven (independent POMs) |
| Config | Spring Cloud Config Server |
| Registry | Eureka |
| Gateway | Spring Cloud Gateway |
| Inter-service communication | Apache Kafka (event-driven, no Feign) |
| Async messaging | Apache Kafka |
| Write DB | PostgreSQL 16 |
| Read DB | MongoDB 7 |
| Migrations | Flyway |
| Auth | JWT (Spring Security) |
| Containers | Docker + Docker Compose |
| Unit tests | JUnit 5 + AssertJ + InMemory adapters |
| Integration tests | Testcontainers |
| E2E tests | RestAssured / WebTestClient |
