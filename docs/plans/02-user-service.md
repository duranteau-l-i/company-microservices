# Phase 2: User Service

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the User bounded context with signup, signin, JWT auth, role-based user management, and CQRS persistence.

**Architecture:** Hexagonal/DDD. Commands → PostgreSQL. Queries → MongoDB. Domain events → Kafka. InMemory adapters for unit tests.

**Tech Stack:** Spring Boot 3.x, Spring Security, JWT (jjwt), JPA/Hibernate, Spring Data MongoDB, Spring Kafka, Flyway, BCrypt

---

### Task 1: Domain Model

**Files:**
- Create: `user-service/pom.xml`
- Create: `user-service/src/main/java/com/company/userservice/domain/model/UserId.java`
- Create: `user-service/src/main/java/com/company/userservice/domain/model/Role.java`
- Create: `user-service/src/main/java/com/company/userservice/domain/model/EmailAddress.java`
- Create: `user-service/src/main/java/com/company/userservice/domain/model/User.java`
- Create: `user-service/src/main/java/com/company/userservice/domain/exception/` (domain exceptions)

- [ ] **Step 1: Create Maven project**
  `pom.xml` with parent `spring-boot-starter-parent`. Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-data-mongodb, spring-kafka, spring-boot-starter-security, spring-boot-starter-actuator, spring-cloud-starter-netflix-eureka-client, spring-cloud-starter-config, flyway-core, jjwt, postgresql, testcontainers, junit5, assertj.

- [ ] **Step 2: Write unit test for UserId value object**
  Test: creation with valid UUID, equality, toString.

- [ ] **Step 3: Implement UserId**
  Record wrapping UUID. Validates non-null. Factory method `UserId.generate()`.

- [ ] **Step 4: Write unit test for Role enum**
  Test: values exist (ADMIN, MANAGER, USER), hierarchy checks.

- [ ] **Step 5: Implement Role**
  Enum with `canCreate(Role target)` method enforcing: ADMIN creates MANAGER/USER, MANAGER creates USER only.

- [ ] **Step 6: Write unit test for EmailAddress value object**
  Test: valid email accepted, invalid rejected, equality, case normalization.

- [ ] **Step 7: Implement EmailAddress**
  Record with validation. Normalizes to lowercase.

- [ ] **Step 8: Write unit test for User aggregate**
  Test: creation with valid data, validation failures (blank name, null role), password hashing check, event emission on creation.

- [ ] **Step 9: Implement User aggregate root**
  Fields: id, email, password (hashed), firstName, lastName, role, createdAt, updatedAt, active. Constructor validates invariants. Factory method `User.create(...)` returns User + raises `UserCreatedEvent`.

- [ ] **Step 10: Implement domain exceptions**
  `UserNotFoundException`, `DuplicateEmailException`, `InsufficientPermissionException`, `InvalidCredentialsException`.

- [ ] **Step 11: Commit**
  `feat(user-service): add domain model with User aggregate, value objects, and exceptions`

---

### Task 2: Domain Events & Ports

**Files:**
- Create: `user-service/src/main/java/com/company/userservice/domain/event/` (events)
- Create: `user-service/src/main/java/com/company/userservice/domain/port/in/` (use cases)
- Create: `user-service/src/main/java/com/company/userservice/domain/port/out/` (repos, publisher)

- [ ] **Step 1: Create domain events**
  `UserCreatedEvent`, `UserUpdatedEvent`, `UserDeletedEvent`. Plain Java records — no framework deps. Include relevant fields (userId, email, role, timestamp).

- [ ] **Step 2: Create driven ports (out)**
  `UserCommandRepository` interface: save, findById, findByEmail, existsByEmail.
  `UserQueryRepository` interface: findById, findAll, search.
  `UserEventPublisher` interface: publish(DomainEvent).

- [ ] **Step 3: Create driving ports (in) — use case interfaces**
  `SignUpUseCase`, `SignInUseCase`, `RefreshTokenUseCase`, `CreateUserUseCase`, `GetUserUseCase`, `ListUsersUseCase`, `UpdateUserUseCase`, `DeleteUserUseCase`.
  Each with a command/query record and result type.

- [ ] **Step 4: Commit**
  `feat(user-service): add domain events and port interfaces`

---

### Task 3: InMemory Adapters & Use Case Tests

**Files:**
- Create: `user-service/src/test/java/com/company/userservice/unit/application/inmemory/InMemoryUserCommandRepository.java`
- Create: `user-service/src/test/java/com/company/userservice/unit/application/inmemory/InMemoryUserQueryRepository.java`
- Create: `user-service/src/test/java/com/company/userservice/unit/application/inmemory/InMemoryUserEventPublisher.java`

- [ ] **Step 1: Implement InMemoryUserCommandRepository**
  `HashMap<UserId, User>` backed. Implements all command repository methods.

- [ ] **Step 2: Implement InMemoryUserQueryRepository**
  `HashMap<UserId, UserReadModel>` backed. Supports search/filter.

- [ ] **Step 3: Implement InMemoryUserEventPublisher**
  `List<DomainEvent>` collector. Accessor + reset methods.

- [ ] **Step 4: Commit**
  `test(user-service): add InMemory adapters for unit testing`

---

### Task 4: Application Layer — Command Handlers

**Files:**
- Create: `user-service/src/main/java/com/company/userservice/application/command/` (handlers)
- Create: `user-service/src/test/java/com/company/userservice/unit/application/command/` (tests)

- [ ] **Step 1: Write tests for SignUpHandler**
  Test: successful signup creates USER, duplicate email rejected, event published.

- [ ] **Step 2: Implement SignUpHandler**
  Implements `SignUpUseCase`. Validates email uniqueness, hashes password, creates User aggregate, persists, publishes event.

- [ ] **Step 3: Write tests for SignInHandler**
  Test: valid credentials return tokens, invalid email rejected, wrong password rejected.

- [ ] **Step 4: Implement SignInHandler**
  Implements `SignInUseCase`. Validates credentials, generates JWT access + refresh tokens.

- [ ] **Step 5: Write tests for CreateUserHandler**
  Test: ADMIN creates MANAGER (success), MANAGER creates USER (success), MANAGER creates MANAGER (rejected), USER creates USER (rejected), duplicate email rejected.

- [ ] **Step 6: Implement CreateUserHandler**
  Implements `CreateUserUseCase`. Checks caller's role permission via `Role.canCreate()`.

- [ ] **Step 7: Write tests for UpdateUserHandler**
  Test: user updates own profile (success), user updates other user (rejected), ADMIN updates any (success).

- [ ] **Step 8: Implement UpdateUserHandler**

- [ ] **Step 9: Write tests for DeleteUserHandler**
  Test: ADMIN deletes user (success), non-ADMIN rejected.

- [ ] **Step 10: Implement DeleteUserHandler**
  Soft delete (set active = false). Publishes `UserDeletedEvent`.

- [ ] **Step 11: Run all unit tests**
  Run: `mvn test -pl user-service -Dtest="unit/**"`
  Expected: all pass.

- [ ] **Step 12: Commit**
  `feat(user-service): add command handlers with signup, signin, and user management`

---

### Task 5: Application Layer — Query Handlers

**Files:**
- Create: `user-service/src/main/java/com/company/userservice/application/query/` (handlers)
- Create: `user-service/src/test/java/com/company/userservice/unit/application/query/` (tests)

- [ ] **Step 1: Write tests for GetUserHandler**
  Test: user gets own profile (full), user gets other profile (restricted based on role).

- [ ] **Step 2: Implement GetUserHandler**

- [ ] **Step 3: Write tests for ListUsersHandler**
  Test: ADMIN/MANAGER list all users, USER cannot list all.

- [ ] **Step 4: Implement ListUsersHandler**

- [ ] **Step 5: Commit**
  `feat(user-service): add query handlers for user retrieval`

---

### Task 6: Infrastructure — PostgreSQL Persistence

**Files:**
- Create: `user-service/src/main/java/com/company/userservice/infrastructure/adapter/out/persistence/command/` (JPA entities, repos, mapper)
- Create: `user-service/src/main/resources/db/migration/V1__create_user_tables.sql`
- Create: `user-service/src/test/java/com/company/userservice/integration/persistence/` (tests)

- [ ] **Step 1: Create Flyway migration**
  `users` table: id (UUID PK), email (unique), password_hash, first_name, last_name, role, active, created_at, updated_at.
  `refresh_tokens` table: id, token_hash, user_id (FK), expires_at, revoked, created_at.

- [ ] **Step 2: Create JPA entities**
  `UserJpaEntity`, `RefreshTokenJpaEntity`. JPA annotations only on these — not on domain objects.

- [ ] **Step 3: Create JPA repositories**
  Spring Data JPA interfaces.

- [ ] **Step 4: Create mapper**
  Domain `User` ↔ `UserJpaEntity` mapping.

- [ ] **Step 5: Create PostgreSQL adapter**
  Implements `UserCommandRepository` port. Uses JPA repo + mapper.

- [ ] **Step 6: Write integration tests**
  Test with Testcontainers PostgreSQL: save/find user, find by email, duplicate email constraint, Flyway migration runs.

- [ ] **Step 7: Commit**
  `feat(user-service): add PostgreSQL persistence adapter with Flyway migrations`

---

### Task 7: Infrastructure — MongoDB Read Model

**Files:**
- Create: `user-service/src/main/java/com/company/userservice/infrastructure/adapter/out/persistence/query/` (documents, repos, mapper)
- Create: `user-service/src/test/java/com/company/userservice/integration/persistence/` (Mongo tests)

- [ ] **Step 1: Create MongoDB document**
  `UserDocument` — denormalized read model: id, email, firstName, lastName, role, active, createdAt.

- [ ] **Step 2: Create MongoDB repository**
  Spring Data MongoDB interface.

- [ ] **Step 3: Create mapper + adapter**
  Implements `UserQueryRepository` port.

- [ ] **Step 4: Write integration tests**
  Test with Testcontainers MongoDB: save/find, search, list.

- [ ] **Step 5: Commit**
  `feat(user-service): add MongoDB read model adapter`

---

### Task 8: Infrastructure — Kafka Events

**Files:**
- Create: `user-service/src/main/java/com/company/userservice/infrastructure/adapter/out/messaging/` (producer)
- Create: `user-service/src/main/java/com/company/userservice/infrastructure/adapter/in/kafka/` (consumer for CQRS sync)
- Create: `user-service/src/test/java/com/company/userservice/integration/kafka/` (tests)

- [ ] **Step 1: Create Kafka producer adapter**
  Implements `UserEventPublisher` port. Wraps events in envelope, serializes to JSON, sends to `user-events` topic.

- [ ] **Step 2: Create CQRS sync consumer**
  Listens to `user-events` topic. On `UserCreatedEvent`/`UserUpdatedEvent`: upserts MongoDB document. On `UserDeletedEvent`: marks inactive. Idempotent via `processed_events` collection.

- [ ] **Step 3: Write integration tests**
  Test with Testcontainers Kafka: publish event → verify it arrives, consume event → verify MongoDB updated.

- [ ] **Step 4: Commit**
  `feat(user-service): add Kafka event publishing and CQRS sync consumer`

---

### Task 9: Infrastructure — Security & JWT

**Files:**
- Create: `user-service/src/main/java/com/company/userservice/infrastructure/security/` (JWT provider, filter, config)
- Create: `user-service/src/test/java/com/company/userservice/integration/security/` (tests)

- [ ] **Step 1: Create JwtTokenProvider**
  Generates access tokens (30 min) and refresh tokens. Validates and parses tokens. Uses `jjwt` library.

- [ ] **Step 2: Create JwtAuthenticationFilter**
  Extends `OncePerRequestFilter`. Extracts Bearer token, validates, sets SecurityContext.

- [ ] **Step 3: Create SecurityConfig**
  Public endpoints: signup, signin, refresh, actuator health. All others require auth. Stateless sessions. Role hierarchy (ADMIN > MANAGER > USER).

- [ ] **Step 4: Create admin seeder**
  `ApplicationRunner` that creates default ADMIN from env vars on first startup if none exists.

- [ ] **Step 5: Write integration tests**
  Test: JWT generation/validation, protected endpoint returns 401 without token, 200 with valid token, admin seeding works.

- [ ] **Step 6: Commit**
  `feat(user-service): add JWT authentication and Spring Security configuration`

---

### Task 10: Infrastructure — REST Controllers

**Files:**
- Create: `user-service/src/main/java/com/company/userservice/infrastructure/adapter/in/rest/` (controllers, DTOs)
- Create: `user-service/src/test/java/com/company/userservice/integration/rest/` (tests)

- [ ] **Step 1: Create request/response DTOs**
  `SignUpRequest`, `SignInRequest`, `CreateUserRequest`, `UpdateUserRequest`, `AuthResponse`, `UserResponse`, `UserSummaryResponse`.

- [ ] **Step 2: Create AuthController**
  `POST /api/users/signup`, `POST /api/users/signin`, `POST /api/users/auth/refresh`. Public endpoints. Calls driving ports.

- [ ] **Step 3: Create UserController**
  `GET /api/users/{id}`, `GET /api/users`, `PUT /api/users/{id}`, `DELETE /api/users/{id}`. Protected endpoints. Calls driving ports.

- [ ] **Step 4: Write integration tests**
  Test with MockMvc: signup flow, signin flow, protected endpoints return 401/403, CRUD operations with correct roles.

- [ ] **Step 5: Commit**
  `feat(user-service): add REST controllers for auth and user management`

---

### Task 11: Spring Configuration & Wiring

**Files:**
- Create: `user-service/src/main/java/com/company/userservice/infrastructure/config/` (bean config)
- Create: `user-service/src/main/java/com/company/userservice/UserServiceApplication.java`
- Create: `user-service/src/main/resources/application.yml`
- Create: `user-service/src/main/resources/bootstrap.yml`

- [ ] **Step 1: Create application class**
  `@SpringBootApplication` + `@EnableDiscoveryClient`.

- [ ] **Step 2: Create bean configuration**
  Wire use case handlers with adapter implementations. `@Configuration` class.

- [ ] **Step 3: Create application.yml**
  Local dev config: embedded DB for quick start, Kafka, MongoDB connection.

- [ ] **Step 4: Create bootstrap.yml**
  Points to config-service for remote config.

- [ ] **Step 5: Verify service starts locally**
  Run with dev profile. Check: registers with Eureka, health endpoint UP.

- [ ] **Step 6: Commit**
  `feat(user-service): add Spring configuration and application wiring`

---

### Task 12: Dockerization

**Files:**
- Create: `user-service/Dockerfile`
- Modify: `docker-compose.yml`
- Modify: `config-repo/user-service.yml`

- [ ] **Step 1: Create Dockerfile**
  Multi-stage: Maven build → JRE runtime. Port 8081.

- [ ] **Step 2: Add to docker-compose.yml**
  With health check, depends_on config-service + registry-service + postgres + mongodb + kafka.

- [ ] **Step 3: Update config-repo/user-service.yml**
  Docker profile with container-internal hostnames.

- [ ] **Step 4: Verify full stack**
  Run: `docker compose up -d`. Verify: user-service registers in Eureka, signup/signin work through host port.

- [ ] **Step 5: Commit**
  `feat(user-service): add Docker support and compose integration`