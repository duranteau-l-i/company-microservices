# Transactional Outbox Pattern

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Guarantee atomicity between aggregate saves and domain event publishing by introducing the transactional outbox pattern across all three services.

---

## Context

Currently, every command handler publishes domain events directly to Kafka after saving the aggregate to PostgreSQL. These are two separate operations with no atomicity guarantee. If the Kafka publish fails (or the process crashes between the two), the event is lost permanently — PostgreSQL is updated but no consumer ever learns about it. There is no event store or replay mechanism.

The outbox pattern fixes this by saving the event to PostgreSQL in the same transaction as the aggregate, then having a background relay forward it to Kafka.

---

## Approach

1. Each command handler that publishes events gains `@Transactional`. Spring proxies the handler through its use-case interface (returned from `@Bean` in `UseCaseConfig`), so the annotation is enforced at runtime without Spring imports in the application layer. Unit tests that use `new Handler(...)` are unaffected.
2. The Kafka publisher is replaced by an `OutboxEventPublisher` that saves a `PENDING` row to a PostgreSQL `outbox_events` table using JPA — participating in the handler's transaction.
3. A scheduled relay (`OutboxRelayScheduler` → `OutboxRelayService`) polls `PENDING` rows, sends each to Kafka via a pre-serialized JSON string, blocks for broker ack, then marks the row `PUBLISHED`. Each row is processed in its own `REQUIRES_NEW` transaction with `FOR UPDATE SKIP LOCKED` to support multi-pod deployments.

The transaction chain:
```
Handler.create()           @Transactional(REQUIRED) — opens TX
  commandRepo.save()       @Transactional(REQUIRED) — joins TX
  outboxPublisher.publish() — JPA save joins TX
TX commits: aggregate + outbox row are atomic
                            ↓
OutboxRelayService.processOne()   @Transactional(REQUIRES_NEW) per row
  SELECT … FOR UPDATE SKIP LOCKED
  OutboxKafkaSender.send(…)       blocks for broker ack
  entry.markPublished()
  outboxRepo.save()
TX commits
```

MongoDB read-model saves remain outside the JPA transaction (unchanged — if they fail, the relay delivers the Kafka event and consumers rebuild the read model).

---

## Files to Create (pattern described once, replicated per service)

### Flyway migrations

- `user-service/src/main/resources/db/migration/V3__add_outbox_events.sql`
- `company-service/src/main/resources/db/migration/V2__add_outbox_events.sql`
- `officer-service/src/main/resources/db/migration/V2__add_outbox_events.sql`

```sql
CREATE TABLE outbox_events (
    id              UUID         PRIMARY KEY,
    aggregate_id    UUID         NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    topic           VARCHAR(255) NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ  NOT NULL,
    published_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status_created_at ON outbox_events (status, created_at);
```

`id` = `event.eventId()` (domain-generated UUID, matching the pattern of all other entities). `payload` = full `EventEnvelope` serialized to JSON — the exact string consumers already parse with `objectMapper.readTree(rawMessage)`.

### `OutboxEventEntity` (in `infrastructure/persistence/command/`)

Fields: `id UUID`, `aggregateId UUID`, `eventType VARCHAR`, `topic VARCHAR`, `payload TEXT`, `status VARCHAR`, `createdAt TIMESTAMPTZ`, `publishedAt TIMESTAMPTZ nullable`. Include a `markPublished(Instant)` method that sets `status = "PUBLISHED"` and `publishedAt`.

### `OutboxEventEntityRepository` (in `infrastructure/persistence/command/`)

```java
public interface OutboxEventEntityRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT :batchSize", nativeQuery = true)
    List<OutboxEventEntity> findPending(@Param("batchSize") int batchSize);

    @Query(value = "SELECT * FROM outbox_events WHERE id = :id AND status = 'PENDING' FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<OutboxEventEntity> findPendingByIdWithLock(@Param("id") UUID id);
}
```

### `OutboxKafkaSender` port interface (in `infrastructure/messaging/`)

Thin interface wrapping the Kafka send so `OutboxRelayService` can be unit-tested without a broker:

```java
public interface OutboxKafkaSender {
    void send(String topic, String key, String payload) throws Exception;
}
```

Production impl `KafkaOutboxSender`: `@Component`, injects `KafkaTemplate<String, String>` (the `outboxKafkaTemplate` bean), calls `.send(...).get(10, TimeUnit.SECONDS)`.

### `OutboxEventPublisher` (in `infrastructure/messaging/`)

Named `OutboxUserEventPublisher` / `OutboxCompanyEventPublisher` / `OutboxOfficerEventPublisher`. Implements the existing event publisher port (`UserEventPublisher`, etc.). Annotated `@Primary @Component`. Injects `OutboxEventEntityRepository`, the `kafkaObjectMapper` bean, and the topic via `@Value`. In `publish(DomainEvent event)`: wrap with `EventEnvelope.wrap(event)`, serialize to JSON string, save an `OutboxEventEntity` with status `PENDING`. No `@Transactional` — relies on the caller's active transaction.

### `OutboxRelayService` (in `infrastructure/messaging/`)

Spring `@Component`. Injects `OutboxEventEntityRepository` and `OutboxKafkaSender`.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void processOne(UUID outboxId) {
    outboxRepository.findPendingByIdWithLock(outboxId).ifPresent(entry -> {
        try {
            kafkaSender.send(entry.getTopic(), entry.getAggregateId().toString(), entry.getPayload());
            entry.markPublished(Instant.now());
            outboxRepository.save(entry);
        } catch (Exception e) {
            log.error("Relay failed for {}, leaving PENDING for retry", outboxId, e);
        }
    });
}
```

### `OutboxRelayScheduler` (in `infrastructure/messaging/`)

Separate class from `OutboxRelayService` to avoid Spring AOP self-invocation bypassing `@Transactional`. Uses `@Scheduled(fixedDelayString = "${app.outbox.relay-interval-ms:5000}")` — `fixedDelay` (not `fixedRate`) to avoid pile-up under Kafka slowness. Polls `outboxRepository.findPending(50)` then iterates calling `relayService.processOne(entry.getId())`.

### `SchedulingConfig` (in `config/`)

```java
@Configuration
@EnableScheduling
public class SchedulingConfig {}
```

---

## Files to Modify

### `KafkaConfig` (in `config/`) — all three services

Add a `KafkaOutboxSender` `@Bean` and a `KafkaTemplate<String, String>` for it. Values use `StringSerializer` so the pre-serialized JSON string is sent without double-encoding:

```java
@Bean
public KafkaTemplate<String, String> outboxKafkaTemplate() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config));
}
```

### Command handlers — add `@Transactional`

Add `@Transactional` (from `org.springframework.transaction.annotation`) to the primary method of each handler that calls `publisher.publish()`:

| Service | Handler | Method |
|---|---|---|
| user-service | `SignUpHandler` | `signUp()` |
| user-service | `CreateUserHandler` | `create()` |
| user-service | `UpdateUserHandler` | `update()` |
| user-service | `DeleteUserHandler` | `delete()` |
| company-service | `CreateCompanyHandler` | `create()` |
| company-service | `UpdateCompanyHandler` | `update()` |
| company-service | `DeleteCompanyHandler` | `delete()` |
| officer-service | `CreateOfficerHandler` | `create()` |
| officer-service | `UpdateOfficerHandler` | `update()` |
| officer-service | `DeleteOfficerHandler` | `delete()` |
| officer-service | `LinkOfficerToCompanyHandler` | `link()` |
| officer-service | `UnlinkOfficerFromCompanyHandler` | `unlink()` |

`SignInHandler`, `RefreshTokenHandler`, and all query handlers do not publish events — leave untouched.

### Existing Kafka publishers — remove `@Component`

Remove `@Component` from `KafkaUserEventPublisher`, `KafkaEventPublisher`, `KafkaOfficerEventPublisher`. These are superseded by the outbox publishers. Delete or keep for reference.

### `config-repo` YML files — all three services

```yaml
app:
  outbox:
    relay-interval-ms: 5000
```

---

## What Does NOT Change

- `UseCaseConfig.java` — Spring auto-wires `OutboxEventPublisher` as `@Primary` for the event publisher port.
- All event classes, `DomainEvent` interface, `EventEnvelope` record.
- All Kafka consumer classes and their idempotency logic — same JSON payload as before.
- All existing unit tests — `InMemoryEventPublisher` still satisfies the port interface; `new Handler(...)` bypasses the proxy.

---

## Unit Tests — InMemory Pattern

### Command handler unit tests (unchanged)

`InMemoryUserEventPublisher` / `InMemoryCompanyEventPublisher` / `InMemoryOfficerEventPublisher` remain as-is in `src/test/java/.../stubs/`.

### New: `InMemoryOutboxEventEntityRepository` (in `stubs/`)

```java
public class InMemoryOutboxEventEntityRepository {
    private final Map<UUID, OutboxEventEntity> store = new LinkedHashMap<>();

    public OutboxEventEntity save(OutboxEventEntity entity) { store.put(entity.getId(), entity); return entity; }
    public Optional<OutboxEventEntity> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
    public List<OutboxEventEntity> findPending(int batchSize) {
        return store.values().stream()
            .filter(e -> "PENDING".equals(e.getStatus()))
            .sorted(Comparator.comparing(OutboxEventEntity::getCreatedAt))
            .limit(batchSize).toList();
    }
    public Optional<OutboxEventEntity> findPendingByIdWithLock(UUID id) {
        return findById(id).filter(e -> "PENDING".equals(e.getStatus()));
    }
    public List<OutboxEventEntity> all() { return List.copyOf(store.values()); }
    public void clear() { store.clear(); }
}
```

### New: `InMemoryOutboxKafkaSender` (in `stubs/`)

```java
public class InMemoryOutboxKafkaSender implements OutboxKafkaSender {
    private final List<SentMessage> sent = new ArrayList<>();

    @Override public void send(String topic, String key, String payload) {
        sent.add(new SentMessage(topic, key, payload));
    }

    public List<SentMessage> sentMessages() { return List.copyOf(sent); }
    public void clear() { sent.clear(); }

    public record SentMessage(String topic, String key, String payload) {}
}
```

### New: `OutboxRelayServiceTest` (no Spring context)

```java
class OutboxRelayServiceTest {
    private InMemoryOutboxEventEntityRepository outboxRepo;
    private InMemoryOutboxKafkaSender kafkaSender;
    private OutboxRelayService relayService;

    @BeforeEach void setUp() {
        outboxRepo = new InMemoryOutboxEventEntityRepository();
        kafkaSender = new InMemoryOutboxKafkaSender();
        relayService = new OutboxRelayService(outboxRepo, kafkaSender);
    }

    @Test void processOne_sendsMessageAndMarksPublished() { ... }
    @Test void processOne_skipsMissingEntry() { ... }
    @Test void processOne_leavesEntryPendingOnKafkaFailure() { ... }
}
```

---

## Implementation Sequence

- [ ] 0. ~~Write this plan to `docs/plans/09-outbox-pattern.md`~~ ✓
- [ ] 1. Flyway migrations — one per service
- [ ] 2. `OutboxEventEntity` + `OutboxEventEntityRepository` per service
- [ ] 3. `OutboxKafkaSender` port + `KafkaOutboxSender` impl + `InMemoryOutboxKafkaSender` stub per service
- [ ] 4. `InMemoryOutboxEventEntityRepository` stub per service
- [ ] 5. `OutboxEventPublisher` per service (`@Primary @Component`)
- [ ] 6. `outboxKafkaTemplate` bean + `KafkaOutboxSender` `@Bean` in `KafkaConfig` per service
- [ ] 7. `OutboxRelayService` + `OutboxRelayScheduler` + `SchedulingConfig` per service
- [ ] 8. Add `@Transactional` to all 12 command handler methods listed above
- [ ] 9. Remove `@Component` from old Kafka publishers
- [ ] 10. Add `app.outbox.relay-interval-ms` to config-repo YMLs
- [ ] 11. `OutboxRelayServiceTest` per service
- [ ] 12. `OutboxEventPublisherIT` + `OutboxRelaySchedulerIT` per service

---

## Verification

### Unit tests (no changes, should stay green)
```bash
mvn test -pl user-service,company-service,officer-service
```

### Integration tests (new)

**`OutboxEventPublisherIT`** (`@DataJpaTest` + Testcontainers Postgres):
- Call `publisher.publish(event)` inside a test transaction — assert one `PENDING` row with correct fields
- Rollback via `TransactionTemplate` — assert zero rows

**`OutboxRelaySchedulerIT`** (Testcontainers Postgres + Kafka):
- Insert `PENDING` row directly, call `relayScheduler.relay()`, assert Kafka received the message and row is `PUBLISHED`

### End-to-end
```bash
docker compose up -d
# Create a company, confirm officer-service's known_companies projection receives CompanyCreatedEvent
```
