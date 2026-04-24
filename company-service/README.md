# company-service

Company management, CRUD operations, and officer read model synchronization for the company microservices platform.

## What this service does

- Owns the **Company** aggregate (name, registrationNumber, address, ownerId, status, officers read model).
- Handles company **CRUD** endpoints with ownership-based authorization.
- Provides **role-based views**: full view (address, officers) for owners/ADMIN/MANAGER; restricted view for non-owners.
- Publishes `CompanyCreatedEvent`, `CompanyUpdatedEvent`, `CompanyDeletedEvent` domain events to the `company-events` Kafka topic.
- Consumes `officer-events` topic to maintain embedded officer summaries in the MongoDB read model (idempotent via `processed_events`).
- Validates JWTs (HS256, shared secret from Config Service) — does not generate tokens.

### Architecture

- **DDD + Hexagonal** — the domain layer has zero framework imports.
- **CQRS** — PostgreSQL is the write store, MongoDB is the query store, Kafka synchronizes them.
- **Ports & Adapters** — driving ports under `domain/port/usecases/`, driven ports under `domain/port/infrastructure/`, adapters under `infrastructure/`.

### Tech stack

Java 21, Spring Boot 3.3, Spring Cloud 2023, Spring Security + JWT, Spring Data JPA, Flyway, Spring Data MongoDB, Spring Kafka, springdoc-openapi, Testcontainers.

## Endpoints

Base path: `/api/companies`

| Method | Path | Public | Description |
|--------|------|:------:|-------------|
| POST | `/` | no | Create a company (any authenticated user except MANAGER) |
| GET | `/{id}` | no | Get company (full or restricted view based on role/ownership) |
| PUT | `/{id}` | no | Update company (owner, MANAGER, or ADMIN) |
| DELETE | `/{id}` | no | Delete company (owner or ADMIN) — returns 204 No Content |
| GET | `/` | no | List companies (USER: own; MANAGER/ADMIN: all) |
| GET | `/search?term=...` | no | Search companies (restricted view, MANAGER/ADMIN only) |

Authenticated requests expect `Authorization: Bearer <accessToken>`.

## OpenAPI / Swagger

Once the service is running (see below), browse:

- Swagger UI: <http://localhost:8082/swagger-ui.html>
- Raw spec (JSON): <http://localhost:8082/v3/api-docs>

The spec declares a `bearerAuth` security scheme — click **Authorize** in Swagger UI and paste a JWT obtained from `/api/users/signin` in the user-service.

## Running locally

### Prerequisites

- Java 21, Maven 3.9+
- Docker + Docker Compose
- A populated `.env` file at the repo root (copy `.env.example` and fill in secrets)

### Dev mode (infra in Docker, service on the host)

Use this loop when iterating on service code. Spring Boot DevTools / your IDE will pick up changes without restarting the whole stack.

```bash
# From the repo root
docker compose -f docker-compose.infra.yml up -d

# From company-service/
KAFKA_BOOTSTRAP_SERVERS=localhost:29092
mvn spring-boot:run
```

The service binds to `http://localhost:8082` and registers with Eureka at `http://localhost:8761`.

Quick smoke test:

```bash
# First sign in via user-service to get a token:
curl -X POST http://localhost:8081/api/users/signin \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"admin123"}'

# Then create a company:
curl -X POST http://localhost:8082/api/companies \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Acme Corp","registrationNumber":"REG-001","street":"1 Main St","city":"Paris","postalCode":"75001","country":"France","ownerDisplayName":"Alice Smith"}'
```

### Prod-like mode (full stack in Docker)

Builds the production image and runs every service inside the Compose network.

```bash
# From the repo root
docker compose up -d --build
```

Service comes up on `http://localhost:8082`; health check at `/actuator/health`.

Stop with `docker compose down` (add `-v` to wipe volumes).

### Configuration

Environment variables consumed at runtime (see `.env.example` for defaults):

| Variable | Purpose |
|----------|---------|
| `POSTGRES_HOST` / `_PORT` / `_USER` / `_PASSWORD` | Write store (`company_db`) |
| `MONGO_HOST` / `_PORT` / `_INITDB_ROOT_USERNAME` / `_INITDB_ROOT_PASSWORD` | Read store (`company_query_db`) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker(s) |
| `CONFIG_SERVER_URI` | Spring Cloud Config endpoint |
| `EUREKA_URI` | Eureka registry URL |
| `JWT_SECRET` | HMAC signing key (>= 256 bits) |

Non-secret configuration lives in `config-repo/company-service.yml` and is fetched from the Config Server at boot.

## Tests

From `company-service/`:

### Unit tests

Pure Java, no Spring, no Mockito — use the InMemory stubs under `src/test/java/.../stubs/`.

```bash
mvn test
```

### Integration tests (Testcontainers)

Spin up real PostgreSQL, MongoDB, and Kafka in Docker for a single JVM run. Requires Docker to be running.

```bash
mvn verify
```

Individual IT classes:

- `PostgresCompanyCommandRepositoryIT` — JPA against `postgres:16`
- `MongoCompanyQueryRepositoryIT` — Spring Data Mongo against `mongo:7`
- `KafkaEventPublisherIT` — producer/consumer round-trip against `confluentinc/cp-kafka`
- `CompanyEventConsumerIT` — CQRS sync (internal Kafka consumer)
- `OfficerEventConsumerIT` — cross-service event handling
- `CompanyControllerIT` — REST layer with `@WebMvcTest`

Run just one:

```bash
mvn test -Dtest=CompanyControllerIT
```

## Troubleshooting

- **Testcontainers cannot talk to Docker**: ensure Docker Desktop is running and `DOCKER_HOST` is unset (or points at a socket the current user can access).
- **Port 8082 in use**: another service or a stale `mvn spring-boot:run` is still up — kill it or pick a different port via `SERVER_PORT=8083`.
- **Flyway checksum mismatch in dev**: Flyway won't auto-repair, run `mvn flyway:repair` against the local Postgres.
- **MongoDB auto-index not created**: set `spring.data.mongodb.auto-index-creation=true` in `application.yml` (already configured by default).
- **Kafka cannot connect in dev mode**: override `KAFKA_BOOTSTRAP_SERVERS=localhost:29092` (not `kafka:9092`).

## Project layout

```
company-service/
├── Dockerfile                     # multi-stage Maven → JRE image
├── pom.xml
└── src/
    ├── main/java/com/company/companyservice/
    │   ├── domain/                # pure domain — no framework imports
    │   │   ├── model/             # Company, CompanyId, Address, CompanyStatus, OfficerSummary, views
    │   │   ├── event/             # CompanyCreatedEvent, CompanyUpdatedEvent, CompanyDeletedEvent
    │   │   ├── exception/         # CompanyNotFoundException, CompanyAccessDeniedException, etc.
    │   │   └── port/
    │   │       ├── usecases/      # driving ports (CreateCompanyUseCase, GetCompanyUseCase, …)
    │   │       └── infrastructure/# driven ports (CompanyCommandRepository, CompanyEventPublisher, …)
    │   ├── application/
    │   │   ├── command/           # CreateCompanyHandler, UpdateCompanyHandler, DeleteCompanyHandler
    │   │   └── query/             # GetCompanyHandler, ListCompaniesHandler, SearchCompaniesHandler
    │   ├── infrastructure/
    │   │   ├── messaging/         # KafkaEventPublisher, EventEnvelope
    │   │   └── persistence/
    │   │       ├── command/       # JPA entity, mapper, PostgresCompanyCommandRepository
    │   │       └── query/         # MongoDB document, mapper, MongoCompanyQueryRepository, processed events
    │   ├── presentation/
    │   │   ├── kafka/             # CompanyEventConsumer, OfficerEventConsumer
    │   │   └── rest/              # CompanyController, RestExceptionHandler, DTOs
    │   ├── config/                # KafkaConfig, UseCaseConfig, OpenApiConfig
    │   └── security/              # JwtTokenValidator, JwtAuthenticationFilter, SecurityConfig
    ├── main/resources/
    │   ├── application.yml
    │   ├── bootstrap.yml
    │   └── db/migration/          # Flyway (V1__create_company_tables.sql)
    └── test/java/.../             # unit, integration trees
```

## See also

- Root `CLAUDE.md` — platform-wide conventions
- `docs/specs/2026-04-19-company-microservices-design.md` — design document
- `docs/plans/04-company-service.md` — implementation plan for this service
