# Phase 4: Company Service

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Company bounded context with CRUD, ownership-based authorization, CQRS persistence, and Kafka event handling.

**Architecture:** Same hexagonal/DDD/CQRS pattern as user-service. PostgreSQL write, MongoDB read. Publishes company events, consumes officer events for read model enrichment.

**Tech Stack:** Spring Boot 3.x, Spring Security, JPA, Spring Data MongoDB, Spring Kafka, Flyway, OpenFeign (prepared for Phase 6)

---

### Task 1: Domain Model

**Files:**
- Create: `company-service/pom.xml`
- Create: `company-service/src/main/java/com/company/companyservice/domain/model/` (CompanyId, Company, Address, CompanyStatus, OfficerSummary)
- Create: `company-service/src/main/java/com/company/companyservice/domain/exception/`

- [ ] **Step 1: Create Maven project**
  Same dependency set as user-service (minus jjwt for token generation — only validation needed). Add OpenFeign + Resilience4j dependencies (used in Phase 6).

- [ ] **Step 2: Write tests for value objects**
  CompanyId (UUID wrapper), Address (street, city, postalCode, country — all required), CompanyStatus enum (ACTIVE, INACTIVE).

- [ ] **Step 3: Implement value objects**

- [ ] **Step 4: Write tests for Company aggregate**
  Test: creation with valid data, validation failures, status transitions, event emission.

- [ ] **Step 5: Implement Company aggregate**
  Fields: id, name, registrationNumber, address, ownerId (UUID — no User dependency), status, createdAt, updatedAt. Factory method raises `CompanyCreatedEvent`.

- [ ] **Step 6: Create OfficerSummary value object**
  Read-only reference: officerId, firstName, lastName, title. Used in MongoDB read model only.

- [ ] **Step 7: Create domain exceptions**
  `CompanyNotFoundException`, `CompanyAccessDeniedException`, `DuplicateRegistrationNumberException`.

- [ ] **Step 8: Commit**
  `feat(company-service): add domain model with Company aggregate and value objects`

---

### Task 2: Events & Ports

**Files:**
- Create: `company-service/src/main/java/com/company/companyservice/domain/event/`
- Create: `company-service/src/main/java/com/company/companyservice/domain/port/in/`
- Create: `company-service/src/main/java/com/company/companyservice/domain/port/out/`

- [ ] **Step 1: Create domain events**
  `CompanyCreatedEvent`, `CompanyUpdatedEvent`, `CompanyDeletedEvent`.

- [ ] **Step 2: Create driven ports**
  `CompanyCommandRepository`, `CompanyQueryRepository`, `CompanyEventPublisher`.

- [ ] **Step 3: Create driving ports**
  `CreateCompanyUseCase`, `GetCompanyUseCase`, `ListCompaniesUseCase`, `SearchCompaniesUseCase`, `UpdateCompanyUseCase`, `DeleteCompanyUseCase`.

- [ ] **Step 4: Commit**
  `feat(company-service): add domain events and port interfaces`

---

### Task 3: InMemory Adapters

**Files:**
- Create: `company-service/src/test/java/com/company/companyservice/unit/application/inmemory/`

- [ ] **Step 1: Implement InMemoryCompanyCommandRepository**
- [ ] **Step 2: Implement InMemoryCompanyQueryRepository**
  Must support: findByOwnerId, findAll, search (by name, partial match).
- [ ] **Step 3: Implement InMemoryCompanyEventPublisher**
- [ ] **Step 4: Commit**
  `test(company-service): add InMemory adapters`

---

### Task 4: Command Handlers

**Files:**
- Create: `company-service/src/main/java/com/company/companyservice/application/command/`
- Create: `company-service/src/test/java/com/company/companyservice/unit/application/command/`

- [ ] **Step 1: Write tests for CreateCompanyHandler**
  Test: USER creates company (ownerId = caller), MANAGER cannot create, event published.

- [ ] **Step 2: Implement CreateCompanyHandler**

- [ ] **Step 3: Write tests for UpdateCompanyHandler**
  Test: owner updates own (success), non-owner USER rejected, MANAGER updates any (success), ADMIN updates any (success).

- [ ] **Step 4: Implement UpdateCompanyHandler**

- [ ] **Step 5: Write tests for DeleteCompanyHandler**
  Test: owner deletes own (success), MANAGER cannot delete, ADMIN deletes any (success), non-owner USER rejected.

- [ ] **Step 6: Implement DeleteCompanyHandler**

- [ ] **Step 7: Commit**
  `feat(company-service): add command handlers with authorization`

---

### Task 5: Query Handlers

**Files:**
- Create: `company-service/src/main/java/com/company/companyservice/application/query/`
- Create: `company-service/src/test/java/com/company/companyservice/unit/application/query/`

- [ ] **Step 1: Write tests for GetCompanyHandler**
  Test: owner sees full view + officers, non-owner USER sees restricted view, MANAGER/ADMIN see full view.

- [ ] **Step 2: Implement GetCompanyHandler**
  Returns full or restricted view based on caller role and ownership.

- [ ] **Step 3: Write tests for ListCompaniesHandler**
  Test: USER lists own companies only, MANAGER/ADMIN list all.

- [ ] **Step 4: Implement ListCompaniesHandler**

- [ ] **Step 5: Write tests for SearchCompaniesHandler**
  Test: returns restricted view for all users (public search).

- [ ] **Step 6: Implement SearchCompaniesHandler**

- [ ] **Step 7: Commit**
  `feat(company-service): add query handlers with role-based views`

---

### Task 6: PostgreSQL Persistence

**Files:**
- Create: `company-service/src/main/java/com/company/companyservice/infrastructure/adapter/out/persistence/command/`
- Create: `company-service/src/main/resources/db/migration/V1__create_company_tables.sql`
- Create: `company-service/src/test/java/com/company/companyservice/integration/persistence/`

- [ ] **Step 1: Create Flyway migration**
  `companies` table: id, name, registration_number (unique), street, city, postal_code, country, owner_id, status, created_at, updated_at.

- [ ] **Step 2: Create JPA entity + mapper + adapter**

- [ ] **Step 3: Write integration tests**
  Testcontainers PostgreSQL: save/find, find by owner, unique registration number constraint.

- [ ] **Step 4: Commit**
  `feat(company-service): add PostgreSQL persistence`

---

### Task 7: MongoDB Read Model

**Files:**
- Create: `company-service/src/main/java/com/company/companyservice/infrastructure/adapter/out/persistence/query/`
- Create: `company-service/src/test/java/com/company/companyservice/integration/persistence/`

- [ ] **Step 1: Create MongoDB document**
  `CompanyDocument`: all company fields + `List<OfficerSummaryDocument>` (embedded). Two projection variants: full and restricted.

- [ ] **Step 2: Create repository + mapper + adapter**

- [ ] **Step 3: Write integration tests**
  Testcontainers MongoDB: save/find, search by name, list by ownerId.

- [ ] **Step 4: Commit**
  `feat(company-service): add MongoDB read model`

---

### Task 8: Kafka Events

**Files:**
- Create: `company-service/src/main/java/com/company/companyservice/infrastructure/adapter/out/messaging/`
- Create: `company-service/src/main/java/com/company/companyservice/infrastructure/adapter/in/kafka/`
- Create: `company-service/src/test/java/com/company/companyservice/integration/kafka/`

- [ ] **Step 1: Create Kafka producer**
  Publishes company events to `company-events` topic.

- [ ] **Step 2: Create CQRS sync consumer**
  Listens to `company-events` for internal read model sync.

- [ ] **Step 3: Create officer events consumer**
  Listens to `officer-events` for: `OfficerLinkedToCompanyEvent` (add officer summary to read model), `OfficerUnlinkedFromCompanyEvent` (remove), `OfficerUpdatedEvent` (refresh).

- [ ] **Step 4: Write integration tests**
  Testcontainers Kafka: publish/consume cycle, officer event updates company read model.

- [ ] **Step 5: Commit**
  `feat(company-service): add Kafka event publishing and consumption`

---

### Task 9: Security, REST Controllers & Wiring

**Files:**
- Create: `company-service/src/main/java/com/company/companyservice/infrastructure/security/`
- Create: `company-service/src/main/java/com/company/companyservice/infrastructure/adapter/in/rest/`
- Create: `company-service/src/main/java/com/company/companyservice/infrastructure/config/`
- Create: `company-service/src/main/java/com/company/companyservice/CompanyServiceApplication.java`
- Create: `company-service/src/main/resources/`
- Create: `company-service/src/test/java/com/company/companyservice/integration/rest/`

- [ ] **Step 1: Create JWT filter + security config**
  Same pattern as user-service — validates token, extracts userId + role. No token generation.

- [ ] **Step 2: Create DTOs**
  Request/response DTOs for all endpoints.

- [ ] **Step 3: Create CompanyController**
  All REST endpoints from the spec. Uses `@PreAuthorize` + ownership checks in use case layer.

- [ ] **Step 4: Create application class + config + wiring**

- [ ] **Step 5: Write integration tests**
  MockMvc: CRUD with auth, role-based access, ownership checks, restricted vs full views.

- [ ] **Step 6: Commit**
  `feat(company-service): add REST controllers, security, and Spring config`

---

### Task 10: Dockerization

**Files:**
- Create: `company-service/Dockerfile`
- Modify: `docker-compose.yml`

- [ ] **Step 1: Create Dockerfile + add to compose**
- [ ] **Step 2: Verify**: registers in Eureka, CRUD works through gateway.
- [ ] **Step 3: Commit**
  `feat(company-service): add Docker support`

---

### Task 11: README & OpenAPI

Create service documentation following the pattern established in other service READMEs.

**Files:**
- Create: `company-service/README.md`, `company-service/src/main/java/com/company/companyservice/infrastructure/config/OpenApiConfig.java`
- Modify: `company-service/pom.xml`, `company-service/src/main/resources/application.yml`, `SecurityConfig.java`

- [ ] **Step 1: Add springdoc-openapi**
  Dependency `springdoc-openapi-starter-webmvc-ui`. Pin version in properties.

- [ ] **Step 2: Wire OpenApiConfig with bearerAuth**
  Title, description, `bearerAuth` scheme, default security requirement.

- [ ] **Step 3: Permit Swagger paths in SecurityConfig**
  `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`.

- [ ] **Step 4: Annotate controllers**
  `@Tag` per controller, `@Operation` per endpoint, `@SecurityRequirements` on anything public.

- [ ] **Step 5: Write README**
  All sections from the skill. Swagger UI at `http://localhost:8082/swagger-ui.html`.

- [ ] **Step 6: Commit**
  `docs(company-service): add README and OpenAPI spec`
