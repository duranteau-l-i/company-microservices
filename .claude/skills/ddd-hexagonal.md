# DDD & Hexagonal Architecture

## When to Use

When creating or modifying domain models, use cases, ports, or adapters in any service.

## Domain Layer Rules

The domain layer (`domain/`) is the core of each service. It must:

- Have **zero framework imports** — no Spring, no JPA, no Kafka, no Jackson annotations
- Contain only pure Java: aggregates, entities, value objects, domain events, exceptions, port interfaces
- Use constructor-based validation — aggregates and value objects validate themselves on creation
- Raise domain events as return values or collected in the aggregate — never publish directly

### Aggregate Design

- Each bounded context has one aggregate root (User, Company, Officer)
- The aggregate root controls all mutations and enforces invariants
- Entities within the aggregate are accessed only through the root
- Value objects are immutable — use Java records where appropriate
- IDs are value objects wrapping UUID, not raw UUID fields

### Port Interfaces

**Driving ports (`port/in/`)** — define use cases the outside world can invoke:
- One interface per use case: `CreateUserUseCase`, `SignInUseCase`
- Input is a command/query record, output is a domain object or projection
- No framework types in signatures (no `ResponseEntity`, no `Page`)

**Driven ports (`port/out/`)** — define what the domain needs from infrastructure:
- `UserCommandRepository` — save, findById, existsByEmail (write store)
- `UserQueryRepository` — search, list, findByFilters (read store)
- `EventPublisher` — publish domain events

## Application Layer Rules

The application layer (`application/`) implements the driving ports:

- Command handlers orchestrate: validate → load aggregate → execute domain logic → persist → publish events
- Query handlers read from the query store (MongoDB) — no domain logic
- Transaction boundaries live here (via Spring `@Transactional` on the adapter side)
- No direct infrastructure imports — depends only on ports

## Infrastructure Layer Rules

The infrastructure layer (`infrastructure/`) implements the driven ports:

- JPA entities are separate from domain entities — map between them
- MongoDB documents are separate from domain models — map between them
- Controllers call driving ports, never domain objects directly
- Each adapter is a Spring `@Component` or `@Repository` implementing a port interface

## Mapping Between Layers

- Domain model <-> JPA entity: mapper in `infrastructure/adapter/out/persistence/command/`
- Domain model <-> MongoDB document: mapper in `infrastructure/adapter/out/persistence/query/`
- Domain model <-> REST DTO: mapper in `infrastructure/adapter/in/rest/`
- Never expose domain objects in REST responses — always map to DTOs