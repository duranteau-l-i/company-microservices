# Phase 5: Officer Service

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Officer bounded context with CRUD, search, deduplication, company linking, and role-based authorization.

**Architecture:** Same hexagonal/DDD/CQRS pattern. Officer aggregate contains CompanyLink entities. Search endpoint supports deduplication workflow.

**Tech Stack:** Spring Boot 3.x, Spring Security, JPA, Spring Data MongoDB, Spring Kafka, Flyway, OpenFeign (prepared for Phase 6)

---

### Task 1: Domain Model

**Files:**
- Create: `officer-service/pom.xml`
- Create: `officer-service/src/main/java/com/company/officerservice/domain/model/` (OfficerId, Officer, CompanyLink, Address)
- Create: `officer-service/src/main/java/com/company/officerservice/domain/exception/`

- [ ] **Step 1: Create Maven project**
  Same dependency pattern. Include OpenFeign + Resilience4j.

- [ ] **Step 2: Write tests for value objects**
  OfficerId, Address (reused pattern but independent definition — no shared lib).

- [ ] **Step 3: Implement value objects**

- [ ] **Step 4: Write tests for CompanyLink entity**
  Test: creation, activation/deactivation, appointment/resignation dates.

- [ ] **Step 5: Implement CompanyLink**
  Fields: companyId, title/role, appointmentDate, resignationDate (nullable), active. Methods: `deactivate()`, `resign(date)`.

- [ ] **Step 6: Write tests for Officer aggregate**
  Test: creation, adding company link, removing link, duplicate link prevention (same companyId + same title), event emission.

- [ ] **Step 7: Implement Officer aggregate**
  Fields: id, firstName, lastName, dateOfBirth, nationality, address, email, phone, companyLinks (List), createdAt, updatedAt. Methods: `linkToCompany(CompanyLink)`, `unlinkFromCompany(companyId)`, `deactivateLinksForCompany(companyId)`.

- [ ] **Step 8: Create domain exceptions**
  `OfficerNotFoundException`, `DuplicateLinkException`, `OfficerAccessDeniedException`.

- [ ] **Step 9: Commit**
  `feat(officer-service): add domain model with Officer aggregate and CompanyLink entity`

---

### Task 2: Events & Ports

**Files:**
- Create: `officer-service/src/main/java/com/company/officerservice/domain/event/`
- Create: `officer-service/src/main/java/com/company/officerservice/domain/port/in/`
- Create: `officer-service/src/main/java/com/company/officerservice/domain/port/out/`

- [ ] **Step 1: Create domain events**
  `OfficerCreatedEvent`, `OfficerUpdatedEvent`, `OfficerDeletedEvent`, `OfficerLinkedToCompanyEvent` (includes officerId, companyId, firstName, lastName, title), `OfficerUnlinkedFromCompanyEvent`.

- [ ] **Step 2: Create driven ports**
  `OfficerCommandRepository`: save, findById, findByNameAndDateOfBirth (for deduplication search).
  `OfficerQueryRepository`: findById, findByCompanyId, search.
  `OfficerEventPublisher`.

- [ ] **Step 3: Create driving ports**
  `CreateOfficerUseCase`, `GetOfficerUseCase`, `SearchOfficersUseCase`, `UpdateOfficerUseCase`, `DeleteOfficerUseCase`, `LinkOfficerToCompanyUseCase`, `UnlinkOfficerFromCompanyUseCase`, `ListOfficersByCompanyUseCase`, `ListCompaniesByOfficerUseCase`.

- [ ] **Step 4: Commit**
  `feat(officer-service): add domain events and port interfaces`

---

### Task 3: InMemory Adapters

**Files:**
- Create: `officer-service/src/test/java/com/company/officerservice/unit/application/inmemory/`

- [ ] **Step 1: Implement InMemoryOfficerCommandRepository**
  Must support `findByNameAndDateOfBirth` for deduplication.
- [ ] **Step 2: Implement InMemoryOfficerQueryRepository**
  Must support `findByCompanyId` and search with partial name match.
- [ ] **Step 3: Implement InMemoryOfficerEventPublisher**
- [ ] **Step 4: Commit**
  `test(officer-service): add InMemory adapters`

---

### Task 4: Command Handlers

**Files:**
- Create: `officer-service/src/main/java/com/company/officerservice/application/command/`
- Create: `officer-service/src/test/java/com/company/officerservice/unit/application/command/`

- [ ] **Step 1: Write tests for CreateOfficerHandler**
  Test: USER (company owner) creates officer, MANAGER creates, non-owner USER rejected, event published.

- [ ] **Step 2: Implement CreateOfficerHandler**

- [ ] **Step 3: Write tests for LinkOfficerToCompanyHandler**
  Test: owner links to own company (success), duplicate link rejected, non-owner rejected, MANAGER links to any (success), event published.

- [ ] **Step 4: Implement LinkOfficerToCompanyHandler**
  Note: companyId validation via Feign is added in Phase 6. For now, trust the companyId.

- [ ] **Step 5: Write tests for UnlinkOfficerFromCompanyHandler**
  Test: owner unlinks from own company, non-owner rejected, event published.

- [ ] **Step 6: Implement UnlinkOfficerFromCompanyHandler**

- [ ] **Step 7: Write tests for UpdateOfficerHandler**
  Test: ADMIN/MANAGER update (success), USER rejected.

- [ ] **Step 8: Implement UpdateOfficerHandler**

- [ ] **Step 9: Write tests for DeleteOfficerHandler**
  Test: ADMIN deletes (success), MANAGER/USER rejected, event published.

- [ ] **Step 10: Implement DeleteOfficerHandler**

- [ ] **Step 11: Commit**
  `feat(officer-service): add command handlers with authorization and linking`

---

### Task 5: Query Handlers

**Files:**
- Create: `officer-service/src/main/java/com/company/officerservice/application/query/`
- Create: `officer-service/src/test/java/com/company/officerservice/unit/application/query/`

- [ ] **Step 1: Write tests for GetOfficerHandler**
  Test: owner sees full view, non-owner sees restricted, MANAGER/ADMIN see full.

- [ ] **Step 2: Implement GetOfficerHandler**

- [ ] **Step 3: Write tests for SearchOfficersHandler**
  Test: search by firstName + lastName + dateOfBirth, partial match, returns matches for deduplication workflow.

- [ ] **Step 4: Implement SearchOfficersHandler**

- [ ] **Step 5: Write tests for ListOfficersByCompanyHandler**
  Test: returns all officers linked to a company.

- [ ] **Step 6: Implement ListOfficersByCompanyHandler**

- [ ] **Step 7: Write tests for ListCompaniesByOfficerHandler**
  Test: returns all companies an officer is linked to.

- [ ] **Step 8: Implement ListCompaniesByOfficerHandler**

- [ ] **Step 9: Commit**
  `feat(officer-service): add query handlers with search and role-based views`

---

### Task 6: PostgreSQL Persistence

**Files:**
- Create: `officer-service/src/main/java/com/company/officerservice/infrastructure/adapter/out/persistence/command/`
- Create: `officer-service/src/main/resources/db/migration/V1__create_officer_tables.sql`
- Create: `officer-service/src/test/java/com/company/officerservice/integration/persistence/`

- [ ] **Step 1: Create Flyway migration**
  `officers` table: id, first_name, last_name, date_of_birth, nationality, street, city, postal_code, country, email, phone, created_at, updated_at.
  `company_links` table: id, officer_id (FK), company_id, title, appointment_date, resignation_date, active, created_at.
  Unique constraint on (officer_id, company_id, title) to prevent duplicate links.

- [ ] **Step 2: Create JPA entities + mapper + adapter**
  `OfficerJpaEntity`, `CompanyLinkJpaEntity`. One-to-many relationship.

- [ ] **Step 3: Write integration tests**
  Testcontainers: save/find, find by name + DOB (deduplication), unique link constraint.

- [ ] **Step 4: Commit**
  `feat(officer-service): add PostgreSQL persistence`

---

### Task 7: MongoDB Read Model

**Files:**
- Create: `officer-service/src/main/java/com/company/officerservice/infrastructure/adapter/out/persistence/query/`
- Create: `officer-service/src/test/java/com/company/officerservice/integration/persistence/`

- [ ] **Step 1: Create MongoDB document**
  `OfficerDocument`: all fields + embedded company link summaries. Full and restricted projections.

- [ ] **Step 2: Create repository + mapper + adapter**
  Search support: by companyId, by name (partial match), by name + DOB.

- [ ] **Step 3: Write integration tests**

- [ ] **Step 4: Commit**
  `feat(officer-service): add MongoDB read model`

---

### Task 8: Kafka Events

**Files:**
- Create: `officer-service/src/main/java/com/company/officerservice/infrastructure/adapter/out/messaging/`
- Create: `officer-service/src/main/java/com/company/officerservice/infrastructure/adapter/in/kafka/`
- Create: `officer-service/src/test/java/com/company/officerservice/integration/kafka/`

- [ ] **Step 1: Create Kafka producer**
  Publishes to `officer-events` topic.

- [ ] **Step 2: Create CQRS sync consumer**
  Internal read model sync.

- [ ] **Step 3: Create company events consumer**
  Listens to `company-events`: on `CompanyDeletedEvent`, deactivates all company links for that company.

- [ ] **Step 4: Write integration tests**

- [ ] **Step 5: Commit**
  `feat(officer-service): add Kafka event publishing and consumption`

---

### Task 9: Security, REST Controllers & Wiring

**Files:**
- Create: `officer-service/src/main/java/com/company/officerservice/infrastructure/security/`
- Create: `officer-service/src/main/java/com/company/officerservice/infrastructure/adapter/in/rest/`
- Create: `officer-service/src/main/java/com/company/officerservice/infrastructure/config/`
- Create: `officer-service/src/main/java/com/company/officerservice/OfficerServiceApplication.java`
- Create: `officer-service/src/main/resources/`
- Create: `officer-service/src/test/java/com/company/officerservice/integration/rest/`

- [ ] **Step 1: Create JWT filter + security config**

- [ ] **Step 2: Create DTOs**
  Request/response for all endpoints including search results and link requests.

- [ ] **Step 3: Create OfficerController**
  All endpoints from spec. Search endpoint returns potential matches for deduplication.

- [ ] **Step 4: Create application class + config + wiring**

- [ ] **Step 5: Write integration tests**
  MockMvc: CRUD, search, link/unlink, authorization matrix, restricted vs full views.

- [ ] **Step 6: Commit**
  `feat(officer-service): add REST controllers, security, and Spring config`

---

### Task 10: Dockerization

**Files:**
- Create: `officer-service/Dockerfile`
- Modify: `docker-compose.yml`

- [ ] **Step 1: Create Dockerfile + add to compose**
- [ ] **Step 2: Verify**: registers in Eureka, CRUD works through gateway.
- [ ] **Step 3: Commit**
  `feat(officer-service): add Docker support`

---

### Task 11: README & OpenAPI

Follow `.claude/skills/service-documentation.md`.

**Files:**
- Create: `officer-service/README.md`, `officer-service/src/main/java/com/company/officerservice/infrastructure/config/OpenApiConfig.java`
- Modify: `officer-service/pom.xml`, `officer-service/src/main/resources/application.yml`, `SecurityConfig.java`

- [ ] **Step 1: Add springdoc-openapi**
  Dependency `springdoc-openapi-starter-webmvc-ui`. Pin version in properties.

- [ ] **Step 2: Wire OpenApiConfig with bearerAuth**
  Title, description, `bearerAuth` scheme, default security requirement.

- [ ] **Step 3: Permit Swagger paths in SecurityConfig**
  `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`.

- [ ] **Step 4: Annotate controllers**
  `@Tag` per controller, `@Operation` per endpoint, `@SecurityRequirements` on anything public.

- [ ] **Step 5: Write README**
  All sections from the skill. Swagger UI at `http://localhost:8083/swagger-ui.html`.

- [ ] **Step 6: Commit**
  `docs(officer-service): add README and OpenAPI spec`
