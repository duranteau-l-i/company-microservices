# user-service

User management, authentication, and JWT issuance for the company microservices platform.

## What this service does

- Owns the **User** aggregate (email, password hash, role, profile fields).
- Handles **sign-up**, **sign-in**, and **refresh-token** flows; issues signed JWT access tokens (30 min) and refresh tokens (7 days).
- Exposes authenticated CRUD endpoints for admins/managers to manage users.
- Seeds a default **ADMIN** user on first startup from `ADMIN_EMAIL` / `ADMIN_PASSWORD` env vars.
- Publishes `UserCreated`, `UserUpdated`, `UserDeleted` domain events to the `user-events` Kafka topic.
- Keeps a MongoDB read model in sync via an internal Kafka consumer (CQRS), with idempotency tracked in `processed_events`.

### Architecture

- **DDD + Hexagonal** — the domain layer has zero framework imports.
- **CQRS** — PostgreSQL is the write store, MongoDB is the query store, Kafka synchronizes them.
- **Ports & Adapters** — driving ports under `domain/port/usecases/`, driven ports under `domain/port/infrastructure/`, adapters under `infrastructure/`.

### Tech stack

Java 21, Spring Boot 3.3, Spring Cloud 2023, Spring Security + `jjwt`, Spring Data JPA, Flyway, Spring Data MongoDB, Spring Kafka, springdoc-openapi, Testcontainers.

## Endpoints

Base path: `/api/users`

| Method | Path               | Public | Description                                        |
| ------ | ------------------ | :----: | -------------------------------------------------- |
| POST   | `/signup`          |  yes   | Register a new user (role USER)                    |
| POST   | `/signin`          |  yes   | Return `{accessToken, refreshToken, expiresIn}`    |
| POST   | `/refresh`         |  yes   | Rotate tokens                                      |
| POST   | `/`                |   no   | Create a user with any role (ADMIN only)           |
| GET    | `/{id}`            |   no   | Fetch a user (self, or ADMIN/MANAGER for any)      |
| GET    | `/?search=...`     |   no   | List/search users (ADMIN/MANAGER)                  |
| PUT    | `/{id}`            |   no   | Update profile (self, or ADMIN for any)            |
| DELETE | `/{id}`            |   no   | Delete a user (ADMIN only)                         |

Authenticated requests expect `Authorization: Bearer <accessToken>`.

## OpenAPI / Swagger

Once the service is running (see below), browse:

- Swagger UI: <http://localhost:8081/swagger-ui.html>
- Raw spec (JSON): <http://localhost:8081/v3/api-docs>

The spec declares a `bearerAuth` security scheme — click **Authorize** in Swagger UI and paste a JWT obtained from `/api/users/signin`.

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

# From user-service/
KAFKA_BOOTSTRAP_SERVERS=localhost:29092 
mvn spring-boot:run
```

The service binds to `http://localhost:8081` and registers with Eureka at `http://localhost:8761`.

Quick smoke test:

```bash
curl -X POST http://localhost:8081/api/users/signup \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"password123","firstName":"Alice","lastName":"Smith"}'
```

### Prod-like mode (full stack in Docker)

Builds the production image and runs every service inside the Compose network.

```bash
# From the repo root
docker compose up -d --build
```

Service comes up on `http://localhost:8081`; health check at `/actuator/health`.

Stop with `docker compose down` (add `-v` to wipe volumes).

### Configuration

Environment variables consumed at runtime (see `.env.example` for defaults):

| Variable                        | Purpose                                          |
| ------------------------------- | ------------------------------------------------ |
| `POSTGRES_HOST` / `_PORT` / `_USER` / `_PASSWORD` | Write store (`user_db`) |
| `MONGO_HOST` / `_PORT` / `_INITDB_ROOT_USERNAME` / `_INITDB_ROOT_PASSWORD` | Read store (`user_query_db`) |
| `KAFKA_BOOTSTRAP_SERVERS`       | Kafka broker(s)                                  |
| `CONFIG_SERVER_URI`             | Spring Cloud Config endpoint                     |
| `EUREKA_URI`                    | Eureka registry URL                              |
| `JWT_SECRET`                    | HMAC signing key (>= 256 bits)                   |
| `JWT_ACCESS_TOKEN_EXPIRATION`   | Access token TTL (ms, default 1800000)           |
| `JWT_REFRESH_TOKEN_EXPIRATION`  | Refresh token TTL (ms, default 604800000)        |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD`| Seeded admin on first startup                    |

Non-secret configuration lives in `config-repo/user-service.yml` and is fetched from the Config Server at boot.

## Tests

From `user-service/`:

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

- `PostgresUserCommandRepositoryIT` — JPA against `postgres:16`
- `MongoUserQueryRepositoryIT` — Spring Data Mongo against `mongo:7`
- `KafkaUserEventPublisherIT` — producer/consumer round-trip against `confluentinc/cp-kafka`

Run just one:

```bash
mvn test -Dtest=PostgresUserCommandRepositoryIT
```

### Troubleshooting

- **Testcontainers cannot talk to Docker**: ensure Docker Desktop is running and `DOCKER_HOST` is unset (or points at a socket the current user can access).
- **Port 8081 in use**: another service or a stale `mvn spring-boot:run` is still up — kill it or pick a different port via `SERVER_PORT=8082`.
- **Flyway checksum mismatch in dev**: Flyway won't auto-repair, run `mvn flyway:repair` against the local Postgres.

## Project layout

```
user-service/
├── Dockerfile                     # multi-stage Maven → JRE image
├── pom.xml
└── src/
    ├── main/java/com/company/userservice/
    │   ├── domain/                # pure domain — no framework imports
    │   ├── application/           # command/query handlers (use cases)
    │   └── infrastructure/        # adapters, Spring config, security
    ├── main/resources/
    │   ├── application.yml
    │   ├── bootstrap.yml
    │   └── db/migration/          # Flyway
    └── test/java/.../             # unit, integration trees
```

## See also

- `docs/specs/2026-04-19-company-microservices-design.md` — design document
- `docs/plans/02-user-service.md` — implementation plan for this service
