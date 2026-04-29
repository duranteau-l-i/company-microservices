# officer-service

Officer management, company linking, and CQRS read model synchronization for the company microservices platform.

## What this service does

- Owns the **Officer** aggregate (personal details, address, active company links).
- Handles officer **CRUD** and **link/unlink** to companies with role-based authorization.
- Provides **role-based views**: full view (all fields + links) for MANAGER/ADMIN; restricted view (name + active links) for USER.
- Publishes `OfficerCreatedEvent`, `OfficerUpdatedEvent`, `OfficerDeletedEvent`, `OfficerLinkedEvent`, `OfficerUnlinkedEvent` to the `officer-events` Kafka topic.
- Consumes `officer-events` (internal) to synchronize the MongoDB read model.
- Consumes `company-events` (external) to handle `CompanyDeletedEvent` — unlinks officers from deleted companies.
- Validates JWTs (HS256, shared secret from Config Service) — does not generate tokens.
- Validates company existence via MongoDB read model (kept current by consuming company events).

## Architecture

DDD + Hexagonal + CQRS — see the [design spec](../docs/specs/2026-04-19-company-microservices-design.md).

## Endpoints

Base path: `/api/officers`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/` | any role | Create an officer and link to a company |
| GET | `/{id}` | any role | Get officer (full view for MANAGER/ADMIN, restricted for USER) |
| PUT | `/{id}` | MANAGER, ADMIN | Update officer personal details |
| DELETE | `/{id}` | ADMIN only | Delete an officer |
| GET | `/search` | any role | Search officers by name or date of birth |
| GET | `/by-company/{companyId}` | any role | List officers active in a company |
| GET | `/{id}/companies` | any role | List all company links for an officer |
| POST | `/{id}/links` | any role | Link an officer to another company |
| DELETE | `/{id}/links/{companyId}` | any role | Unlink an officer from a company |

See [Swagger UI](http://localhost:8083/swagger-ui.html) for full request/response schemas.

## OpenAPI / Swagger

- Swagger UI: `http://localhost:8083/swagger-ui.html`
- Raw spec: `http://localhost:8083/v3/api-docs`

All endpoints require a JWT bearer token. In Swagger UI, click **Authorize** and paste the JWT obtained from `/api/users/signin`.

## Running locally

### Dev mode (infra in Docker, service on host)

```bash
# From repo root
docker compose -f docker-compose.infra.yml up -d

# From officer-service/
KAFKA_BOOTSTRAP_SERVERS=localhost:29092 mvn spring-boot:run
```

### Prod-like mode (full stack)

```bash
# From repo root
docker compose up -d --build
```

Service is available at `http://localhost:8083`.

## Configuration

Environment variables read by the service:

| Variable | Purpose | Default |
|----------|---------|---------|
| `POSTGRES_HOST` | PostgreSQL hostname | `localhost` |
| `POSTGRES_PORT` | PostgreSQL port | `5432` |
| `POSTGRES_USER` | PostgreSQL username | `admin` |
| `POSTGRES_PASSWORD` | PostgreSQL password | `local-dev-password` |
| `MONGO_HOST` | MongoDB hostname | `localhost` |
| `MONGO_PORT` | MongoDB port | `27017` |
| `MONGO_INITDB_ROOT_USERNAME` | MongoDB username | `admin` |
| `MONGO_INITDB_ROOT_PASSWORD` | MongoDB password | `local-dev-password` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker(s) | `localhost:29092` |
| `JWT_SECRET` | Shared JWT signing secret | — |

See `.env.example` at the repo root and `config-repo/officer-service.yml` for full configuration.

## Tests

```bash
# Unit tests (InMemory adapters, no Spring context)
mvn test

# All tests including integration (Testcontainers — Docker required)
mvn verify

# Single test class
mvn test -Dtest=CreateOfficerHandlerTest
mvn verify -Dit.test=OfficerControllerIT
```

## Troubleshooting

- **Docker not running**: Testcontainers requires Docker — start Docker Desktop before running `mvn verify`.
- **Port 8083 already in use**: find and stop the process with `lsof -i :8083`.
- **Flyway checksum mismatch**: a migration file was edited after being applied — recreate the officer_db database or run `mvn flyway:repair`.
- **Config Service unreachable**: in dev mode, start infra with `docker compose -f docker-compose.infra.yml up -d` before starting the service.
- **Kafka not resolving**: in dev mode, override `KAFKA_BOOTSTRAP_SERVERS=localhost:29092`. The config server provides `kafka:9092`, which only resolves inside Docker.

## Project layout

```
officer-service/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/java/com/company/officerservice/
    │   ├── domain/           # Zero framework imports
    │   │   ├── model/        # Officer aggregate, OfficerId, CompanyLink, Address, views
    │   │   ├── event/        # Domain events (OfficerCreatedEvent, etc.)
    │   │   ├── exception/    # Domain exceptions
    │   │   └── port/
    │   │       ├── usecases/       # Driving ports (use case interfaces)
    │   │       └── infrastructure/ # Driven ports (repos, publisher)
    │   ├── application/
    │   │   ├── command/      # 5 command handlers
    │   │   └── query/        # 4 query handlers
    │   ├── infrastructure/
    │   │   ├── messaging/    # KafkaOfficerEventPublisher
    │   │   └── persistence/
    │   │       ├── command/  # JPA entities, OfficerJpaRepository, mapper
    │   │       └── query/    # MongoDB documents, MongoOfficerQueryRepository, mapper
    │   ├── presentation/
    │   │   ├── consumer/     # OfficerEventConsumer, CompanyEventConsumer
    │   │   └── controller/   # OfficerController, DTOs, OfficerDtoMapper
    │   ├── config/           # UseCaseConfig, OpenApiConfig, KafkaConfig
    │   └── security/         # JwtAuthenticationFilter, JwtTokenValidator, SecurityConfig
    ├── main/resources/
    │   ├── db/migration/     # Flyway: V1__create_officer_tables.sql
    │   ├── application.yml   # Local dev defaults + test profile
    │   └── bootstrap.yml     # Config Server connection
    └── test/
        ├── unit/             # Handler tests (InMemory stubs)
        ├── integration/      # Testcontainers (PostgreSQL, MongoDB, Kafka, REST)
        └── stubs/            # InMemory implementations of driven ports
```

## See also

- [Design spec](../docs/specs/2026-04-19-company-microservices-design.md)
- [Implementation plan](../docs/plans/)
