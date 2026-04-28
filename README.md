# Company Microservices Platform

A platform for managing companies and their officers, built with Java 21, Spring Boot 3.x, and Spring Cloud. Six independent services communicate via Kafka (async) and OpenFeign (sync), with PostgreSQL as the write store and MongoDB as the read store.

## Services

| Service          | Port | Description                                          |
| ---------------- | ---- | ---------------------------------------------------- |
| config-service   | 8888 | Spring Cloud Config Server — serves all config files |
| registry-service | 8761 | Eureka registry — service discovery                  |
| api-gateway      | 8080 | Single entry point, JWT validation, routing          |
| user-service     | 8081 | Users, auth, JWT issuance                            |
| company-service  | 8082 | Companies and officer read model                     |
| officer-service  | 8083 | Officers and company links                           |

## Prerequisites

- Docker Desktop (or Docker Engine + Compose plugin) — must be running
- Java 21 + Maven 3.9 — only needed for dev mode (running a service on the host)
- A populated `.env` file at the repo root (see below)

## Environment setup

```bash
cp .env.example .env
# Edit .env and replace every "change-me" value
```

Key variables:

| Variable | Purpose |
| -------- | ------- |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | PostgreSQL root credentials |
| `MONGO_INITDB_ROOT_USERNAME` / `MONGO_INITDB_ROOT_PASSWORD` | MongoDB root credentials |
| `JWT_SECRET` | HMAC signing key — must be at least 256 bits (32 random chars minimum) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Default admin user seeded by user-service on first startup |

## Running the platform

### Full stack (everything in Docker)

Builds all service images and starts the complete platform.

```bash
docker compose up -d --build
```

Bring it down (keep volumes):

```bash
docker compose down
```

Bring it down and wipe all data:

```bash
docker compose down -v
```

### Dev mode (infra only, service on the host)

Use this when iterating on a single service. Start the shared infrastructure first:

```bash
docker compose -f docker-compose.infra.yml up -d
```

Then run the service you are working on from its directory:

```bash
cd user-service
mvn spring-boot:run
```

Repeat for whichever service you need. The service registers with Eureka and connects to the Dockerised databases automatically.

### Startup order

Docker Compose manages startup order via health checks. The sequence is:

```
postgres + mongodb + zookeeper
        ↓
      kafka
        ↓
  config-service
        ↓
  registry-service
        ↓
  api-gateway + user-service + company-service + officer-service
```

Business services wait for config-service to be healthy before they boot, so the first `docker compose up` can take 2–3 minutes.

## Accessing the services

### API Gateway (single entry point)

All external requests go through the gateway:

```
http://localhost:8080/api/users/**
http://localhost:8080/api/companies/**
http://localhost:8080/api/officers/**
```

Services are also reachable directly on their own port during development.

### Swagger UI (per service)

| Service         | Swagger UI                              | Raw spec                              |
| --------------- | --------------------------------------- | ------------------------------------- |
| user-service    | http://localhost:8081/swagger-ui.html   | http://localhost:8081/v3/api-docs     |
| company-service | http://localhost:8082/swagger-ui.html   | http://localhost:8082/v3/api-docs     |
| officer-service | http://localhost:8083/swagger-ui.html   | http://localhost:8083/v3/api-docs     |

To authenticate in Swagger UI: call `POST /api/users/signin`, copy the `accessToken`, then click **Authorize** and paste it.

### Eureka dashboard

```
http://localhost:8761
```

Lists all registered service instances and their health status.

### Health checks

Each service exposes an actuator health endpoint:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

## Accessing the databases

### PostgreSQL

Three databases are created automatically on first startup by `scripts/init-postgres.sql`:

| Database     | Used by          |
| ------------ | ---------------- |
| `user_db`    | user-service     |
| `company_db` | company-service  |
| `officer_db` | officer-service  |

Connect with any PostgreSQL client (psql, DBeaver, DataGrip):

```
Host:     localhost
Port:     5432
User:     <POSTGRES_USER from .env>
Password: <POSTGRES_PASSWORD from .env>
```

Via psql:

```bash
psql -h localhost -U admin -d user_db
```

Via Docker:

```bash
docker exec -it postgres psql -U admin -d user_db
```

### MongoDB

Three databases, each created on first write by its service:

| Database          | Used by          |
| ----------------- | ---------------- |
| `user_query_db`   | user-service     |
| `company_query_db`| company-service  |
| `officer_query_db`| officer-service  |

Connect with mongosh or a GUI (MongoDB Compass, DataGrip):

```
Host:     localhost
Port:     27017
Username: <MONGO_INITDB_ROOT_USERNAME from .env>
Password: <MONGO_INITDB_ROOT_PASSWORD from .env>
```

Via mongosh:

```bash
mongosh "mongodb://admin:change-me@localhost:27017"
```

Via Docker:

```bash
docker exec -it mongodb mongosh -u admin -p local-dev-password
```

### Kafka

The broker is reachable from the host on port `29092` (internal Docker traffic uses `9092`):

```
Bootstrap server (host): localhost:29092
Bootstrap server (Docker network): kafka:9092
```

Topics are created automatically when services first publish events:

| Topic           | Producer         | Consumers                          |
| --------------- | ---------------- | ---------------------------------- |
| `user-events`   | user-service     | company-service                    |
| `company-events`| company-service  | officer-service                    |
| `officer-events`| officer-service  | company-service                    |

List topics via Docker:

```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
```

Consume a topic:

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic user-events \
  --from-beginning
```

## Quick smoke test

```bash
# 1. Sign up
curl -s -X POST http://localhost:8080/api/users/signup \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"password123","firstName":"Alice","lastName":"Smith"}' | jq

# 2. Sign in and capture the access token
TOKEN=$(curl -s -X POST http://localhost:8080/api/users/signin \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"password123"}' | jq -r '.accessToken')

# 3. Fetch own profile
curl -s http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN" | jq
```

## Troubleshooting

- **Services fail to start — "config-service not healthy"**: Config Service needs a minute to build on first run. Run `docker compose logs config-service` to check progress.
- **Port already in use**: Another process is listening on the conflicting port. Find it with `lsof -i :<port>` and kill it, or change the host port in `docker-compose.yml`.
- **Flyway checksum mismatch**: A migration file was edited after it was applied. Connect to the database and run `DELETE FROM flyway_schema_history WHERE checksum != <expected>`, then restart. Or drop the database and let Flyway recreate it (`docker compose down -v`).
- **Testcontainers / Docker not reachable**: Ensure Docker Desktop is running and `DOCKER_HOST` is either unset or points at a reachable socket (`unix:///var/run/docker.sock` on macOS/Linux).
- **Kafka messages not consumed**: Confirm the consumer group is registered — `docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list`.

## Project layout

```
company-microservices/
├── config-service/          # Spring Cloud Config Server
├── registry-service/        # Eureka Service Registry
├── api-gateway/             # Spring Cloud Gateway
├── user-service/            # User bounded context
├── company-service/         # Company bounded context
├── officer-service/         # Officer bounded context
├── config-repo/             # Git-backed config files served by config-service
├── scripts/
│   └── init-postgres.sql    # Creates user_db, company_db, officer_db on startup
├── docs/
│   ├── specs/               # Architecture and design documents
│   └── plans/               # Per-service implementation plans
├── docker-compose.yml       # Full stack (includes infra)
├── docker-compose.infra.yml # Infrastructure only (postgres, mongo, kafka, config, registry)
└── .env.example             # Environment variable template
```

## See also

- `docs/specs/2026-04-19-company-microservices-design.md` — full design specification
- `docs/plans/` — per-service implementation plans
- Each service has its own `README.md` with endpoint docs, config reference, and test instructions