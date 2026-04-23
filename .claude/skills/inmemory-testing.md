# InMemory Testing Pattern

## When to Use

When writing unit tests for domain logic or application/use case handlers. This is the **only** pattern for unit tests in this project. Never use mocks (Mockito, etc.) for unit tests.

## Principle

Instead of mocking repository interfaces, create concrete in-memory implementations backed by `HashMap`. These implementations live in the test source tree and implement the same port interfaces as the real adapters.

## InMemory Adapter Structure

Place in-memory adapters under `src/test/java/.../unit/application/stubs/`:

```
test/
└── unit/
    └── application/
        └── stubs/
            ├── InMemoryUserCommandRepository.java
            ├── InMemoryUserQueryRepository.java
            └── InMemoryEventPublisher.java
```

## How to Build an InMemory Adapter

### Repository Adapter

- Implements the `infrastructure/` port interface
- Backed by `HashMap<UUID, DomainEntity>` or `HashMap<UserId, User>`
- All CRUD operations work on the map
- Query methods (findByEmail, search) iterate and filter the map
- Simulates real behavior: `findById` returns `Optional.empty()` if not found
- No Spring, no annotations — pure Java

### Event Publisher Adapter

- Implements the `EventPublisher` port
- Collects events in a `List<DomainEvent>`
- Provides accessor methods for test assertions: `getPublishedEvents()`, `getLastEvent()`
- Reset method to clear between tests

## Writing Unit Tests

### Test Structure

1. **Arrange**: create InMemory adapters, instantiate the use case handler with them, seed test data directly into the maps
2. **Act**: call the use case handler
3. **Assert**: check the return value, check the state in the InMemory adapter, check published events

### Key Rules

- No Spring context (`@SpringBootTest`) — instantiate handlers with `new`
- No `@MockBean`, `@Mock`, `Mockito.when()` — ever
- Test domain invariants: what happens when you create a user with a duplicate email? The InMemory repo can check this.
- Test event emission: after creating a company, assert that `InMemoryEventPublisher.getPublishedEvents()` contains a `CompanyCreatedEvent`
- Test authorization logic if it lives in the use case handler

### When InMemory Is Not Enough

Some things cannot be tested with InMemory:
- SQL-specific behavior (joins, constraints) → integration test with Testcontainers
- MongoDB query operators → integration test
- Kafka serialization/deserialization → integration test
- HTTP request validation → integration test with MockMvc

For these, use integration tests — not mocks.
