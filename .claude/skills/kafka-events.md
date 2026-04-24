# Kafka Event Handling

## When to Use

When implementing event publishing, consuming, or CQRS read model synchronization in any service.

## Topics

| Topic | Producer | Consumers |
|---|---|---|
| `user-events` | user-service | company-service |
| `company-events` | company-service | officer-service |
| `officer-events` | officer-service | company-service |

## Event Envelope

Every event follows this standard structure:

```json
{
  "eventId": "uuid",
  "eventType": "UserCreatedEvent",
  "aggregateId": "uuid",
  "aggregateType": "User",
  "timestamp": "2026-04-19T10:30:00Z",
  "version": 1,
  "payload": { }
}
```

- `eventId` is a UUID generated at publish time — used for idempotency
- `aggregateId` is used as the Kafka partition key — ensures ordering per aggregate
- `version` is for schema evolution — consumers ignore unknown fields

## Publishing Events

### Domain Side

The domain layer defines event classes in `domain/event/`. These are plain Java records — no Kafka dependencies.

The `EventPublisher` port interface is in `domain/port/infrastructure/`.

### Infrastructure Side

The Kafka producer adapter in `infrastructure/messaging/`:
- Implements the `EventPublisher` port
- Wraps domain events in the envelope structure
- Serializes to JSON (Jackson)
- Sends to the appropriate topic with `aggregateId` as the key
- Uses Spring Kafka `KafkaTemplate`

## Consuming Events

### Internal Consumers (CQRS sync)

In `presentation/consumer/`:
- Listen to the service's own topic for read model sync
- Update MongoDB documents based on event type
- Check `eventId` against `processed_events` collection before processing (idempotency)
- Use `@KafkaListener` with the service's consumer group

### External Consumers (cross-service)

Same location, listening to other services' topics:
- company-service listens to `officer-events` for officer summary updates
- officer-service listens to `company-events` for company deletion handling

## Consumer Group Strategy

- Each service has one consumer group: `<service-name>-group`
- Every service gets every event independently
- Partitioning by `aggregateId` ensures ordering per aggregate

## Idempotency

Each service maintains a `processed_events` collection/table:
- Before processing an event, check if `eventId` exists
- If yes, skip (already processed)
- If no, process and store the `eventId`
- This handles Kafka redeliveries safely

## Error Handling

- Failed event processing: log error, do NOT commit offset — Kafka will redeliver
- Poison messages (undeserializable): send to a dead-letter topic, log, continue
- Configure `ErrorHandler` with retry + dead-letter topic in Spring Kafka config

## Testing

- Unit tests: use `InMemoryEventPublisher` — no Kafka involved
- Integration tests: use Testcontainers Kafka — verify real serialization and consumption
