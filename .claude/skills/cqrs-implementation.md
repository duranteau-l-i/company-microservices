# CQRS Implementation

## When to Use

When implementing command/query separation, read model synchronization, or data persistence in any service.

## Architecture

```
Command Path:  REST Controller → Command Handler → Domain → PostgreSQL → Kafka Event
Query Path:    REST Controller → Query Handler → MongoDB
Sync Path:     Kafka Event → Internal Consumer → MongoDB (read model update)
```

## Write Side (PostgreSQL)

- Source of truth for all domain state
- JPA entities map to/from domain aggregates
- Schema managed by Flyway migrations in `src/main/resources/db/migration/`
- Migration naming: `V1__create_user_table.sql`, `V2__add_user_role_column.sql`
- Transactions managed with `@Transactional` on the persistence adapter

### Command Flow

1. Controller receives request, maps to command record
2. Command handler loads aggregate from PostgreSQL via command repository port
3. Domain logic executes on the aggregate
4. Handler persists updated aggregate to PostgreSQL
5. Handler publishes domain event(s) via event publisher port
6. Kafka producer adapter sends event to topic

## Read Side (MongoDB)

- Denormalized, optimized for query patterns
- Documents are flat projections of domain state — no normalization
- Each query endpoint has a purpose-built document structure
- No domain logic in queries — pure data retrieval

### Read Model Documents

Design MongoDB documents to match exactly what each API endpoint needs to return. Examples:
- `CompanyReadModel`: includes company fields + embedded officer summaries
- `UserReadModel`: includes user profile fields needed for listing/searching

## Synchronization

When a command modifies state:
1. Domain event published to Kafka
2. An internal Kafka consumer in the same service listens for the event
3. Consumer updates/creates/deletes the corresponding MongoDB document
4. Consumer is idempotent — tracks `eventId` in a `processed_events` collection

### Why Kafka (Not ApplicationEvents)

- **Durability**: failed read model updates are retried from Kafka
- **Replay**: read model can be rebuilt from event history
- **Consistency**: same mechanism for internal and cross-service events

## Eventual Consistency

The read model may lag behind the write model by milliseconds to seconds. This is acceptable because:
- Commands return the result from the write side (PostgreSQL), not the read side
- Queries return from MongoDB which may be slightly stale
- For "read your own writes" scenarios: the command response includes the created/updated resource
