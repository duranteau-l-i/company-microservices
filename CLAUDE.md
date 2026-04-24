# Company Microservices Platform

## Project Overview

Microservices platform for managing companies and their officers. Java 21, Spring Boot 3.x, Spring Cloud ecosystem. Monorepo with fully independent services.

## Architecture

- **DDD + Hexagonal Architecture** — domain layer has zero framework imports
- **CQRS** — PostgreSQL (write/command), MongoDB (read/query), synced via Kafka
- **Each service is fully independent** — no shared parent POM, no shared library
- **Inter-service communication** — Kafka (async), OpenFeign + Resilience4j (sync with fault tolerance)

## Services

| Service | Port | Purpose |
|---|---|---|
| config-service | 8888 | Spring Cloud Config Server |
| registry-service | 8761 | Eureka Service Registry |
| api-gateway | 8080 | Spring Cloud Gateway (JWT validation, routing) |
| user-service | 8081 | User management, auth, JWT |
| company-service | 8082 | Company CRUD, officer read model |
| officer-service | 8083 | Officer CRUD, company linking |

## Tech Stack

- Java 21, Spring Boot 3.x, Maven (independent POMs per service)
- PostgreSQL 16, MongoDB 7, Apache Kafka
- Spring Security + JWT, OpenFeign + Resilience4j, Flyway
- Docker + Docker Compose, Testcontainers

## Development Skills

These skills define how to implement specific patterns in this project. Use them when working on the corresponding aspect. They should be continuously improved as the project evolves.

@.claude/skills/ddd-hexagonal.md
@.claude/skills/cqrs-implementation.md
@.claude/skills/inmemory-testing.md
@.claude/skills/kafka-events.md
@.claude/skills/spring-security-jwt.md
@.claude/skills/feign-resilience.md
@.claude/skills/docker-service.md
@.claude/skills/service-documentation.md
@.claude/skills/postman.md

## Key References

- Design spec: `docs/specs/2026-04-19-company-microservices-design.md`
- Implementation plans: `docs/plans/`

## Development Workflow

1. Read the relevant skill before implementing a pattern
2. Follow TDD: write failing test first, implement, verify, commit
3. Unit tests use InMemory adapters — never mocks
4. Integration tests use Testcontainers
5. Each commit should be small and focused

## Commit Conventions

- Use conventional commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`
- Scope by service: `feat(user-service): add signup endpoint`
- No Co-Authored-By lines

## Testing Rules

- **Unit tests**: InMemory pattern, no Spring context, no mocks. Pure Java + JUnit 5 + AssertJ.
- **Integration tests**: Testcontainers for PostgreSQL, MongoDB, Kafka. Spring Boot Test.
- **E2E tests**: Full stack via Docker Compose. RestAssured or WebTestClient.
- Never mock repositories or external adapters in unit tests — use InMemory implementations of the ports.

## Project Structure per Service

```
<service>/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/java/com/company/<service>/
    │   ├── domain/           # Pure domain — no framework imports
    │   │   ├── model/        # Aggregates, Entities, Value Objects
    │   │   ├── event/        # Domain events
    │   │   ├── exception/    # Domain exceptions
    │   │   └── port/
    │   │       ├── usecases/       # Driving ports (use cases)
    │   │       └── infrastructure/ # Driven ports (repos, messaging)
    │   ├── application/
    │   │   ├── command/      # Command handlers (write side)
    │   │   └── query/        # Query handlers (read side)
    │   ├── infrastructure/
    │   │   ├── messaging/    # Kafka producers
    │   │   └── persistence/
    │   │       ├── command/  # JPA entities, repos, mappers
    │   │       └── query/    # MongoDB documents, repos, mappers
    │   ├── presentation/     # Inbound adapters
    │   │   ├── consumer/     # Kafka consumers
    │   │   └── controller/   # REST controllers, DTOs
    │   ├── config/           # Spring beans, config
    │   └── security/         # JWT filter, auth
    ├── main/resources/
    │   ├── db/migration/     # Flyway (PostgreSQL)
    │   └── bootstrap.yml     # Config Server connection
    └── test/
        ├── unit/             # InMemory adapters, domain + app tests
        ├── integration/      # Testcontainers
        └── e2e/              # Full HTTP tests
```
