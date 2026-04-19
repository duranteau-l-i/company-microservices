# Phase 6: Integration & End-to-End

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire cross-service communication (Feign + Resilience4j), implement fault tolerance, and create end-to-end tests that validate the full system.

**Architecture:** Synchronous Feign calls between company ↔ officer services with circuit breaker fallbacks. Kafka events already flowing from Phases 4-5. E2E tests exercise complete user flows through the gateway.

**Tech Stack:** OpenFeign, Resilience4j, WireMock (integration), RestAssured (E2E), Docker Compose

---

### Task 1: Company → Officer Feign Client

**Files:**
- Create: `company-service/src/main/java/com/company/companyservice/infrastructure/adapter/out/feign/OfficerClient.java`
- Create: `company-service/src/main/java/com/company/companyservice/infrastructure/adapter/out/feign/OfficerClientFallbackFactory.java`
- Create: `company-service/src/main/java/com/company/companyservice/infrastructure/adapter/out/feign/OfficerClientDto.java`
- Modify: `company-service/src/main/java/com/company/companyservice/application/query/GetCompanyHandler.java`
- Create: `company-service/src/test/java/com/company/companyservice/integration/feign/`

- [ ] **Step 1: Create Feign client interface**
  `@FeignClient(name = "officer-service")`. Method: `getOfficersByCompanyId(companyId)` → returns list of local DTOs.

- [ ] **Step 2: Create a port interface for the officer fetch**
  `OfficerQueryPort` in `domain/port/out/`: `findOfficersByCompanyId(companyId)`. This keeps Feign out of the domain.

- [ ] **Step 3: Create Feign adapter implementing the port**
  Calls the Feign client, maps response to domain-compatible types.

- [ ] **Step 4: Create FallbackFactory**
  On failure: returns empty list + logs the error. The query handler checks and adds a warning to the response.

- [ ] **Step 5: Configure Resilience4j circuit breaker**
  In `application.yml`: sliding window 10, failure rate 50%, open 30s, half-open 3 calls.

- [ ] **Step 6: Modify GetCompanyHandler**
  Fetch officers via the port. If result indicates fallback (empty + warning), add `warnings` field to response.

- [ ] **Step 7: Write integration tests with WireMock**
  Test: officer-service responds normally → officers included. Officer-service returns 500 → fallback (no officers + warning). Officer-service times out → circuit breaker opens.

- [ ] **Step 8: Commit**
  `feat(company-service): add Feign client for officer fetching with circuit breaker`

---

### Task 2: Officer → Company Feign Client

**Files:**
- Create: `officer-service/src/main/java/com/company/officerservice/infrastructure/adapter/out/feign/CompanyClient.java`
- Create: `officer-service/src/main/java/com/company/officerservice/infrastructure/adapter/out/feign/CompanyClientFallbackFactory.java`
- Modify: `officer-service/src/main/java/com/company/officerservice/application/command/LinkOfficerToCompanyHandler.java`
- Create: `officer-service/src/test/java/com/company/officerservice/integration/feign/`

- [ ] **Step 1: Create Feign client + port interface**
  `CompanyValidationPort` in `domain/port/out/`: `companyExists(companyId)`.

- [ ] **Step 2: Create Feign adapter**
  Calls company-service to check if company exists.

- [ ] **Step 3: Create FallbackFactory**
  On failure: throws `ServiceUnavailableException`. Linking must fail if company cannot be verified.

- [ ] **Step 4: Modify LinkOfficerToCompanyHandler**
  Before linking, call `companyValidationPort.companyExists(companyId)`. If service unavailable, return 503.

- [ ] **Step 5: Write integration tests with WireMock**
  Test: company exists → link succeeds. Company not found → link rejected. Company-service down → 503.

- [ ] **Step 6: Commit**
  `feat(officer-service): add Feign client for company validation with circuit breaker`

---

### Task 3: Update InMemory Adapters for New Ports

**Files:**
- Modify: `company-service/src/test/java/.../inmemory/` (add InMemoryOfficerQueryPort)
- Modify: `officer-service/src/test/java/.../inmemory/` (add InMemoryCompanyValidationPort)

- [ ] **Step 1: Create InMemoryOfficerQueryPort**
  Configurable responses for unit tests: return officer list, return empty (simulating fallback).

- [ ] **Step 2: Create InMemoryCompanyValidationPort**
  Configurable: return true (exists), return false (not found), throw exception (service down).

- [ ] **Step 3: Update existing unit tests**
  Wire new InMemory ports into handler tests that now depend on them.

- [ ] **Step 4: Run all unit tests**
  All services: `mvn test` per service.

- [ ] **Step 5: Commit**
  `test: update InMemory adapters for inter-service ports`

---

### Task 4: End-to-End Test Suite

**Files:**
- Create: `e2e-tests/pom.xml`
- Create: `e2e-tests/src/test/java/com/company/e2e/` (test classes)
- Create: `e2e-tests/src/test/resources/docker-compose-test.yml` (or reuse main compose)

- [ ] **Step 1: Create E2E test project**
  Standalone Maven project. Dependencies: RestAssured, JUnit 5, AssertJ. Runs against full Docker Compose stack.

- [ ] **Step 2: Write auth flow tests**
  - Signup → receive JWT
  - Signin → receive JWT
  - Access protected endpoint without token → 401
  - Access protected endpoint with expired token → 401
  - Refresh token → new access token

- [ ] **Step 3: Write user management tests**
  - Admin creates manager → success
  - Manager creates user → success
  - Manager creates manager → 403
  - User creates user → 403
  - Admin deletes user → success
  - Non-admin deletes user → 403

- [ ] **Step 4: Write company CRUD tests**
  - User creates company → success
  - User views own company → full view
  - User views other's company → restricted view
  - Owner updates company → success
  - Non-owner updates → 403
  - Manager updates any → success
  - Owner deletes → success
  - Manager deletes → 403
  - Admin deletes → success

- [ ] **Step 5: Write officer CRUD tests**
  - User creates officer → success
  - Search by name + DOB → returns matches
  - Link officer to own company → success
  - Link duplicate → rejected
  - View officer in own company → full
  - View officer in other company → restricted
  - Unlink officer → success

- [ ] **Step 6: Write cross-service tests**
  - Get company with officers → officers included
  - Stop officer-service → get company → returns without officers + warning
  - Restart officer-service → circuit breaker recovers → officers included again
  - Delete company → officer links deactivated (via Kafka, eventual consistency — poll with timeout)
  - Link officer to non-existent company → rejected

- [ ] **Step 7: Commit**
  `test: add end-to-end test suite covering all user flows and cross-service scenarios`

---

### Task 5: Final Validation

- [ ] **Step 1: Run all unit tests across all services**
  `mvn test -pl user-service,company-service,officer-service`

- [ ] **Step 2: Run all integration tests**
  `mvn verify -pl user-service,company-service,officer-service -P integration`

- [ ] **Step 3: Start full Docker Compose stack**
  `docker compose up -d`
  Verify: all services healthy, registered in Eureka, accessible through gateway.

- [ ] **Step 4: Run E2E tests**
  `mvn test -pl e2e-tests`
  All pass.

- [ ] **Step 5: Commit**
  `chore: final validation — all tests pass across all layers`
