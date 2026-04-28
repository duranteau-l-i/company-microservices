# Pure Event-Driven Microservices — Remove Inter-Service HTTP

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove every synchronous HTTP/Feign call between business services and replace each with a Kafka-driven local read model, so all cross-service communication flows through events.

**Architecture:** Two existing Feign edges go away. (1) `company-service → officer-service` for fetching officers becomes a direct read of the `CompanyFullView` MongoDB document, which already embeds officer summaries via `OfficerEventConsumer`. (2) `officer-service → company-service` for validating a company's existence becomes a lookup against a new local `known_companies` MongoDB projection populated by a `CompanyEventConsumer` listening to `CompanyCreatedEvent` / `CompanyDeletedEvent`. Validation now relies on eventual consistency — clients retry if the projection has not caught up. Spring Cloud OpenFeign and Resilience4j are removed entirely from both services.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Kafka, Spring Data MongoDB, JUnit 5 + AssertJ, Testcontainers, Maven.

---

## Context

The codebase declares itself event-driven (CQRS over Kafka, MongoDB read models) but two synchronous HTTP edges remain:

- **`company-service → officer-service`** (`OfficerClient`, `GET /api/officers/by-company/{id}`) — used by `GetCompanyHandler` to enrich `GET /api/companies/{id}` with officers, with a graceful-degradation fallback that returns empty officers + a `"Officer service temporarily unavailable"` warning. **This call is already redundant**: `CompanyDocument.officers` (the `CompanyFullView` MongoDB document) already embeds the same officer summaries via `OfficerEventConsumer.handleOfficerLinked` / `handleOfficerUnlinked`. The Feign call adds latency, coupling, a circuit breaker, ThreadLocal fallback bookkeeping, and a `warnings` field to the public API for nothing.

- **`officer-service → company-service`** (`CompanyClient`, `GET /api/companies/{id}`) — used by `LinkOfficerToCompanyHandler` to fail-fast if the target company does not exist, throwing `ServiceUnavailableException` (HTTP 503) when company-service is down. There is currently **no local projection** of companies in officer-service; the only company event consumed is `CompanyDeletedEvent` for cascading unlinks.

The user wants pure event-driven communication. The first edge collapses with no new events (the read model is already maintained). The second edge requires a new `known_companies` projection in officer-service, fed by `CompanyCreatedEvent` and `CompanyDeletedEvent`. The `OfficerEventConsumer` in company-service is also extended to react to `OfficerUpdatedEvent` (refresh embedded `firstName`/`lastName`) and `OfficerDeletedEvent` (remove the officer from every company's embedded list) — a pre-existing staleness bug that becomes more visible once the read model is the only source of truth.

User-confirmed trade-offs:

- **Eventual consistency on link**: if a client links an officer to a company immediately after creating it, officer-service's projection may not yet contain the new id; the link fails with `404 CompanyNotFoundException` and the client retries. No saga, no synchronous wait.
- **`warnings` field removed** from `CompanyFullView`, `GetCompanyUseCase.Result`, and `CompanyFullResponse` — the only producer of that field was the Feign fallback.
- **Stale-summary fix included**: `OfficerEventConsumer` in company-service handles `OfficerUpdatedEvent` and `OfficerDeletedEvent` as part of this work.

---

## File Structure

### company-service

**Create**
- `company-service/src/test/java/com/company/companyservice/unit/presentation/consumer/OfficerEventConsumerTest.java` — unit test for the new consumer event handling (only created if missing; otherwise extend the existing IT)

**Modify**
- `company-service/src/main/java/com/company/companyservice/application/query/GetCompanyHandler.java` — drop `OfficerQueryPort`, return officers directly from `CompanyQueryRepository`
- `company-service/src/main/java/com/company/companyservice/domain/port/usecases/GetCompanyUseCase.java` — drop `warnings` from `Result`
- `company-service/src/main/java/com/company/companyservice/domain/model/CompanyFullView.java` — unchanged structure (officers stays); just verify
- `company-service/src/main/java/com/company/companyservice/presentation/controller/CompanyController.java` — drop `result.warnings()` argument from mapper call
- `company-service/src/main/java/com/company/companyservice/presentation/controller/CompanyDtoMapper.java` — drop `warnings` mapper overload
- `company-service/src/main/java/com/company/companyservice/presentation/controller/CompanyFullResponse.java` — drop `warnings` field
- `company-service/src/main/java/com/company/companyservice/presentation/consumer/OfficerEventConsumer.java` — handle `OfficerUpdatedEvent` and `OfficerDeletedEvent`
- `company-service/src/main/java/com/company/companyservice/domain/port/infrastructure/CompanyQueryRepository.java` — add `findCompaniesContainingOfficer(UUID officerId)` for the new bulk-update path
- `company-service/src/main/java/com/company/companyservice/infrastructure/persistence/query/MongoCompanyQueryRepository.java` — implement the new finder
- `company-service/src/main/java/com/company/companyservice/config/UseCaseConfig.java` — drop `OfficerQueryPort` arg from `getCompanyUseCase` bean
- `company-service/src/main/java/com/company/companyservice/CompanyServiceApplication.java` — drop `@EnableFeignClients`
- `company-service/pom.xml` — drop `spring-cloud-starter-openfeign`, `spring-cloud-starter-circuitbreaker-resilience4j`, `wiremock-jetty12`
- `company-service/src/main/resources/application.yml` — drop `feign:` and `resilience4j:` blocks
- `company-service/src/test/java/com/company/companyservice/integration/messaging/OfficerEventConsumerIT.java` — add cases for updated/deleted handling
- `company-service/src/test/java/com/company/companyservice/integration/controller/CompanyControllerIT.java` — drop `warnings` assertions, drop `OfficerQueryPort` test bean if present
- `company-service/src/test/java/com/company/companyservice/unit/application/query/GetCompanyHandlerTest.java` — drop `OfficerQueryPort` use, drop fallback test, seed officers via `CompanyFullView`

**Delete**
- `company-service/src/main/java/com/company/companyservice/domain/port/infrastructure/OfficerQueryPort.java`
- `company-service/src/main/java/com/company/companyservice/infrastructure/feign/` (entire directory: `OfficerClient.java`, `OfficerClientAdapter.java`, `OfficerClientDto.java`, `OfficerClientFallbackFactory.java`, `OfficerCompanyLinkDto.java`)
- `company-service/src/main/java/com/company/companyservice/config/FeignConfig.java`
- `company-service/src/test/java/com/company/companyservice/stubs/InMemoryOfficerQueryPort.java`
- `company-service/src/test/java/com/company/companyservice/integration/feign/OfficerClientIT.java`
- `company-service/src/test/java/com/company/companyservice/unit/infrastructure/feign/` (entire directory: `OfficerClientAdapterTest.java`, `OfficerClientFallbackFactoryTest.java`)

### officer-service

**Create**
- `officer-service/src/main/java/com/company/officerservice/infrastructure/persistence/query/KnownCompanyDocument.java` — `@Document("known_companies")` with `@Id UUID id`
- `officer-service/src/main/java/com/company/officerservice/infrastructure/persistence/query/KnownCompanyMongoRepository.java` — Spring Data `MongoRepository<KnownCompanyDocument, UUID>`
- `officer-service/src/main/java/com/company/officerservice/infrastructure/persistence/query/MongoCompanyValidationAdapter.java` — `@Component` implementing `CompanyValidationPort`
- `officer-service/src/test/java/com/company/officerservice/integration/messaging/CompanyEventConsumerIT.java` — Testcontainers Kafka + Mongo, asserts `known_companies` updates and cascade-unlink

**Modify**
- `officer-service/src/main/java/com/company/officerservice/presentation/consumer/CompanyEventConsumer.java` — handle `CompanyCreatedEvent` (insert into `known_companies`); on `CompanyDeletedEvent` also delete from `known_companies` (in addition to the existing officer-unlink behaviour)
- `officer-service/src/main/java/com/company/officerservice/application/command/LinkOfficerToCompanyHandler.java` — unchanged behaviour (still calls `CompanyValidationPort.companyExists`); just rebound to the new adapter via DI
- `officer-service/src/main/java/com/company/officerservice/OfficerServiceApplication.java` — drop `@EnableFeignClients`
- `officer-service/pom.xml` — drop `spring-cloud-starter-openfeign`, `spring-cloud-starter-circuitbreaker-resilience4j`, `wiremock-jetty12`
- `officer-service/src/main/resources/application.yml` — drop `feign:` and `resilience4j:` blocks
- `officer-service/src/test/java/com/company/officerservice/stubs/InMemoryCompanyValidationPort.java` — drop `simulateUnavailable` flag
- `officer-service/src/test/java/com/company/officerservice/unit/application/command/LinkOfficerToCompanyHandlerTest.java` — drop `linkRejected_whenCompanyServiceUnavailable` test, keep the `linkRejected_whenCompanyDoesNotExist` test (now backed by an empty projection)
- `officer-service/src/test/java/com/company/officerservice/integration/controller/OfficerControllerIT.java` — keep using `InMemoryCompanyValidationPort` test bean (already in place); just stop calling `setSimulateUnavailable`

**Delete**
- `officer-service/src/main/java/com/company/officerservice/infrastructure/feign/` (entire directory: `CompanyClient.java`, `CompanyClientAdapter.java`, `CompanyClientDto.java`, `CompanyClientFallbackFactory.java`)
- `officer-service/src/main/java/com/company/officerservice/config/FeignConfig.java`
- `officer-service/src/main/java/com/company/officerservice/domain/exception/ServiceUnavailableException.java`
- `officer-service/src/main/java/com/company/officerservice/presentation/controller/RestExceptionHandler.java` — only the `serviceUnavailable` handler method (lines 30–34); keep the rest of the file
- `officer-service/src/test/java/com/company/officerservice/integration/feign/CompanyClientIT.java`
- `officer-service/src/test/java/com/company/officerservice/unit/infrastructure/feign/` (entire directory: `CompanyClientAdapterTest.java`, `CompanyClientFallbackFactoryTest.java`)

### Architecture doc

**Modify**
- `docs/specs/2026-04-19-company-microservices-design.md` — note in the inter-service comm section that there are no Feign edges (sweep for any other mention)

---

## Tasks

> Convention: every task ends with `mvn -pl <service> verify` (unit + IT) and a focused commit. Run from repo root unless noted. Use the existing project module names (`company-service`, `officer-service`).

### Task 1: Branch baseline & no-op build

**Files:**
- Read: `company-service/pom.xml`, `officer-service/pom.xml`

- [ ] **Step 1: Confirm working branch**

```bash
git status -sb
```

Expected: on `events-driven`, no uncommitted business changes (the existing `M .idea/misc.xml` is unrelated and should be left alone or stashed).

- [ ] **Step 2: Run baseline build to confirm a green starting point**

```bash
mvn -pl company-service,officer-service verify
```

Expected: BUILD SUCCESS for both modules. If anything is red on `main`, fix or note it before proceeding — every later step assumes a green baseline.

- [ ] **Step 3: No commit yet** — proceed to Task 2.

---

### Task 2: company-service — drop the Feign call from `GetCompanyHandler`

**Files:**
- Modify: `company-service/src/test/java/com/company/companyservice/unit/application/query/GetCompanyHandlerTest.java`
- Modify: `company-service/src/main/java/com/company/companyservice/application/query/GetCompanyHandler.java`
- Modify: `company-service/src/main/java/com/company/companyservice/domain/port/usecases/GetCompanyUseCase.java`

- [ ] **Step 1: Update the unit test to drop the port and assert officers come from the read model**

Replace the entire body of `GetCompanyHandlerTest.java` with:

```java
package com.company.companyservice.unit.application.query;

import com.company.companyservice.application.query.GetCompanyHandler;
import com.company.companyservice.domain.exception.CompanyNotFoundException;
import com.company.companyservice.domain.model.Address;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.model.CompanyStatus;
import com.company.companyservice.domain.model.OfficerSummary;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.usecases.GetCompanyUseCase;
import com.company.companyservice.stubs.InMemoryCompanyQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetCompanyHandlerTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final CompanyId COMPANY_ID = CompanyId.generate();

    private InMemoryCompanyQueryRepository queryRepo;
    private GetCompanyHandler handler;

    @BeforeEach
    void setUp() {
        queryRepo = new InMemoryCompanyQueryRepository();
        handler = new GetCompanyHandler(queryRepo);
    }

    private CompanyFullView seed(List<OfficerSummary> officers) {
        CompanyFullView seeded = new CompanyFullView(
                COMPANY_ID,
                "Acme Corp",
                "REG-001",
                new Address("1 Main St", "Paris", "75001", "France"),
                OWNER_ID,
                "Alice Smith",
                CompanyStatus.ACTIVE,
                Instant.now(),
                Instant.now(),
                officers
        );
        queryRepo.save(seeded);
        return seeded;
    }

    @Test
    void adminSeesFullView() {
        seed(List.of());
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(UUID.randomUUID(), Role.ADMIN, COMPANY_ID);

        GetCompanyUseCase.Result result = handler.get(query);

        assertThat(result.view()).isInstanceOf(CompanyFullView.class);
    }

    @Test
    void managerSeesFullView() {
        seed(List.of());
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(UUID.randomUUID(), Role.MANAGER, COMPANY_ID);

        GetCompanyUseCase.Result result = handler.get(query);

        assertThat(result.view()).isInstanceOf(CompanyFullView.class);
    }

    @Test
    void ownerSeesFullView() {
        seed(List.of());
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(OWNER_ID, Role.USER, COMPANY_ID);

        GetCompanyUseCase.Result result = handler.get(query);

        assertThat(result.view()).isInstanceOf(CompanyFullView.class);
    }

    @Test
    void nonOwnerSeesRestrictedView() {
        seed(List.of());
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(UUID.randomUUID(), Role.USER, COMPANY_ID);

        GetCompanyUseCase.Result result = handler.get(query);

        assertThat(result.view()).isInstanceOf(CompanyRestrictedView.class);
    }

    @Test
    void fullViewIncludesEmbeddedOfficers() {
        OfficerSummary officer = new OfficerSummary(UUID.randomUUID(), "John", "Doe", "Director");
        seed(List.of(officer));

        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(OWNER_ID, Role.USER, COMPANY_ID);
        GetCompanyUseCase.Result result = handler.get(query);

        assertThat(result.view()).isInstanceOf(CompanyFullView.class);
        CompanyFullView full = (CompanyFullView) result.view();
        assertThat(full.officers()).containsExactly(officer);
    }

    @Test
    void notFound() {
        CompanyId unknownId = CompanyId.generate();
        GetCompanyUseCase.Query query = new GetCompanyUseCase.Query(UUID.randomUUID(), Role.USER, unknownId);

        assertThatThrownBy(() -> handler.get(query))
                .isInstanceOf(CompanyNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run the test — it should fail compilation**

```bash
mvn -pl company-service test-compile
```

Expected: compile error: `GetCompanyHandler` constructor still expects `OfficerQueryPort`; `Result` still has `warnings()`. That's the failing state we want.

- [ ] **Step 3: Trim `GetCompanyUseCase.Result` to drop warnings**

Replace `company-service/src/main/java/com/company/companyservice/domain/port/usecases/GetCompanyUseCase.java`:

```java
package com.company.companyservice.domain.port.usecases;

import com.company.companyservice.domain.model.CompanyId;
import com.company.companyservice.domain.model.CompanyView;
import com.company.companyservice.domain.model.Role;

import java.util.UUID;

public interface GetCompanyUseCase {
    Result get(Query query);

    record Query(UUID callerId, Role callerRole, CompanyId companyId) {}

    record Result(CompanyView view) {}
}
```

- [ ] **Step 4: Rewrite `GetCompanyHandler` to read directly from the repository**

Replace `company-service/src/main/java/com/company/companyservice/application/query/GetCompanyHandler.java`:

```java
package com.company.companyservice.application.query;

import com.company.companyservice.domain.exception.CompanyNotFoundException;
import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.model.Role;
import com.company.companyservice.domain.port.infrastructure.CompanyQueryRepository;
import com.company.companyservice.domain.port.usecases.GetCompanyUseCase;

public class GetCompanyHandler implements GetCompanyUseCase {

    private final CompanyQueryRepository queryRepo;

    public GetCompanyHandler(CompanyQueryRepository queryRepo) {
        this.queryRepo = queryRepo;
    }

    @Override
    public Result get(Query query) {
        CompanyFullView full = queryRepo.findFullById(query.companyId())
                .orElseThrow(() -> new CompanyNotFoundException(query.companyId()));

        boolean isOwner = full.ownerId().equals(query.callerId());
        boolean canSeeFull = query.callerRole().isAtLeast(Role.MANAGER) || isOwner;

        if (!canSeeFull) {
            CompanyRestrictedView restricted = new CompanyRestrictedView(
                    full.id(), full.name(), full.registrationNumber(),
                    full.ownerId(), full.ownerDisplayName(), full.status()
            );
            return new Result(restricted);
        }

        return new Result(full);
    }
}
```

- [ ] **Step 5: Run the test — it should pass**

```bash
mvn -pl company-service -Dtest=GetCompanyHandlerTest test
```

Expected: 6 tests pass, 0 failures.

- [ ] **Step 6: Commit**

Do not commit yet — `UseCaseConfig`, `CompanyController`, and the controller IT still reference the old shape. They are wired in Task 3.

---

### Task 3: company-service — propagate the API change through wiring & controller

**Files:**
- Modify: `company-service/src/main/java/com/company/companyservice/config/UseCaseConfig.java`
- Modify: `company-service/src/main/java/com/company/companyservice/presentation/controller/CompanyController.java`
- Modify: `company-service/src/main/java/com/company/companyservice/presentation/controller/CompanyDtoMapper.java`
- Modify: `company-service/src/main/java/com/company/companyservice/presentation/controller/CompanyFullResponse.java`
- Modify: `company-service/src/test/java/com/company/companyservice/integration/controller/CompanyControllerIT.java`

- [ ] **Step 1: Update `UseCaseConfig` bean for `getCompanyUseCase`**

Open `company-service/src/main/java/com/company/companyservice/config/UseCaseConfig.java` and replace the `getCompanyUseCase` bean (lines ~49–53) with:

```java
    @Bean
    public GetCompanyUseCase getCompanyUseCase(CompanyQueryRepository queryRepository) {
        return new GetCompanyHandler(queryRepository);
    }
```

Then remove the unused import `import com.company.companyservice.domain.port.infrastructure.OfficerQueryPort;`.

- [ ] **Step 2: Update `CompanyFullResponse` to drop `warnings`**

Replace `CompanyFullResponse.java`:

```java
package com.company.companyservice.presentation.controller;

import java.util.List;

public record CompanyFullResponse(
        String id,
        String name,
        String registrationNumber,
        AddressResponse address,
        String ownerId,
        String ownerDisplayName,
        String status,
        String createdAt,
        String updatedAt,
        List<OfficerSummaryResponse> officers
) {
}
```

- [ ] **Step 3: Update `CompanyDtoMapper` to drop the warnings overload**

Replace `CompanyDtoMapper.java`:

```java
package com.company.companyservice.presentation.controller;

import com.company.companyservice.domain.model.CompanyFullView;
import com.company.companyservice.domain.model.CompanyRestrictedView;
import com.company.companyservice.domain.model.OfficerSummary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CompanyDtoMapper {

    public CompanyFullResponse toFullResponse(CompanyFullView view) {
        AddressResponse addressResponse = new AddressResponse(
                view.address().street(),
                view.address().city(),
                view.address().postalCode(),
                view.address().country()
        );

        List<OfficerSummaryResponse> officers = view.officers() == null
                ? List.of()
                : view.officers().stream().map(this::toOfficerSummaryResponse).toList();

        return new CompanyFullResponse(
                view.id().value().toString(),
                view.name(),
                view.registrationNumber(),
                addressResponse,
                view.ownerId().toString(),
                view.ownerDisplayName(),
                view.status().name(),
                view.createdAt().toString(),
                view.updatedAt().toString(),
                officers
        );
    }

    public CompanyRestrictedResponse toRestrictedResponse(CompanyRestrictedView view) {
        return new CompanyRestrictedResponse(
                view.id().value().toString(),
                view.name(),
                view.registrationNumber(),
                view.ownerId().toString(),
                view.ownerDisplayName(),
                view.status().name()
        );
    }

    private OfficerSummaryResponse toOfficerSummaryResponse(OfficerSummary officer) {
        return new OfficerSummaryResponse(
                officer.officerId().toString(),
                officer.firstName(),
                officer.lastName(),
                officer.title()
        );
    }
}
```

- [ ] **Step 4: Update `CompanyController.get` to drop the warnings argument**

Replace lines 91–96 of `CompanyController.java` (the `get` method body that uses `result.warnings()`):

```java
        GetCompanyUseCase.Result result = getCompanyUseCase.get(query);
        Object response = switch (result.view()) {
            case CompanyFullView full -> mapper.toFullResponse(full);
            case CompanyRestrictedView restricted -> mapper.toRestrictedResponse(restricted);
        };
        return ResponseEntity.ok(response);
```

- [ ] **Step 5: Update `CompanyControllerIT` to remove `OfficerQueryPort` test bean and `warnings` assertions**

In `company-service/src/test/java/com/company/companyservice/integration/controller/CompanyControllerIT.java`:

1. Remove every import and bean override referencing `OfficerQueryPort` or `InMemoryOfficerQueryPort`.
2. In any test that previously seeded officers via `officerQueryPort.addOfficer(...)`, replace that with seeding officers directly into the repository under `CompanyFullView.officers`. If the test uses an InMemory repository stub, set the officers list at construction time. If it uses `MongoCompanyQueryRepository` via Testcontainers, write a `CompanyDocument` with `officers` populated.
3. Remove every assertion that reads `.warnings` from the JSON response. Update `CompanyFullResponse` deserializers (Jackson) — no warnings means no field; tests should not look for it.

If a specific test was named `getCompany_returnsWarning_whenOfficerServiceUnavailable` (or similar), delete it — that scenario is no longer possible.

- [ ] **Step 6: Run the company-service compile + unit tests**

```bash
mvn -pl company-service test
```

Expected: PASS. If a test references `OfficerQueryPort` somewhere not yet caught, fix it now.

- [ ] **Step 7: Commit**

```bash
git add company-service/src/main/java/com/company/companyservice/application/query/GetCompanyHandler.java \
        company-service/src/main/java/com/company/companyservice/domain/port/usecases/GetCompanyUseCase.java \
        company-service/src/main/java/com/company/companyservice/config/UseCaseConfig.java \
        company-service/src/main/java/com/company/companyservice/presentation/controller/CompanyController.java \
        company-service/src/main/java/com/company/companyservice/presentation/controller/CompanyDtoMapper.java \
        company-service/src/main/java/com/company/companyservice/presentation/controller/CompanyFullResponse.java \
        company-service/src/test/java/com/company/companyservice/unit/application/query/GetCompanyHandlerTest.java \
        company-service/src/test/java/com/company/companyservice/integration/controller/CompanyControllerIT.java
git commit -m "refactor(company-service): read embedded officers from CompanyFullView, drop warnings"
```

---

### Task 4: company-service — extend `OfficerEventConsumer` to handle update & delete

**Files:**
- Test: `company-service/src/test/java/com/company/companyservice/integration/messaging/OfficerEventConsumerIT.java`
- Modify: `company-service/src/main/java/com/company/companyservice/domain/port/infrastructure/CompanyQueryRepository.java`
- Modify: `company-service/src/main/java/com/company/companyservice/infrastructure/persistence/query/MongoCompanyQueryRepository.java`
- Modify: `company-service/src/main/java/com/company/companyservice/presentation/consumer/OfficerEventConsumer.java`

- [ ] **Step 1: Add a failing IT case for `OfficerUpdatedEvent`**

Open `OfficerEventConsumerIT.java` and add a new `@Test` after the existing linked/unlinked cases. Use the same envelope shape the existing test already builds. Skeleton:

```java
@Test
void officerUpdatedEvent_refreshesEmbeddedFirstNameLastName_onAllCompaniesContainingOfficer() throws Exception {
    UUID companyId = UUID.randomUUID();
    UUID officerId = UUID.randomUUID();
    seedCompanyWithOfficer(companyId, officerId, "Old", "Name", "Director"); // helper that writes a CompanyDocument

    String envelope = """
        {"eventId":"%s","eventType":"OfficerUpdatedEvent","aggregateId":"%s","aggregateType":"Officer","timestamp":"2026-04-26T10:00:00Z","version":1,
         "payload":{"aggregateId":"%s","firstName":"New","lastName":"Name","email":"x@y.z","phone":null,"dateOfBirth":"1990-01-01","nationality":"FR"}}
        """.formatted(UUID.randomUUID(), officerId, officerId);

    sendToOfficerTopic(envelope); // existing helper used by other tests
    awaitConsumerCommit();

    CompanyFullView updated = queryRepository.findFullById(CompanyId.of(companyId)).orElseThrow();
    assertThat(updated.officers()).singleElement().satisfies(o -> {
        assertThat(o.officerId()).isEqualTo(officerId);
        assertThat(o.firstName()).isEqualTo("New");
        assertThat(o.lastName()).isEqualTo("Name");
        assertThat(o.title()).isEqualTo("Director"); // title unchanged
    });
}
```

If `seedCompanyWithOfficer` and `sendToOfficerTopic` helpers are not yet present, mirror the patterns already in this test class. (The existing `OfficerEventConsumerIT` already builds envelopes by hand and writes to the topic; reuse those helpers verbatim.)

- [ ] **Step 2: Add a failing IT case for `OfficerDeletedEvent`**

Add a second `@Test`:

```java
@Test
void officerDeletedEvent_removesOfficerFromAllCompanies() throws Exception {
    UUID companyA = UUID.randomUUID();
    UUID companyB = UUID.randomUUID();
    UUID officerId = UUID.randomUUID();
    seedCompanyWithOfficer(companyA, officerId, "Alice", "S", "Director");
    seedCompanyWithOfficer(companyB, officerId, "Alice", "S", "Treasurer");

    String envelope = """
        {"eventId":"%s","eventType":"OfficerDeletedEvent","aggregateId":"%s","aggregateType":"Officer","timestamp":"2026-04-26T10:00:00Z","version":1,
         "payload":{"aggregateId":"%s","firstName":"Alice","lastName":"S"}}
        """.formatted(UUID.randomUUID(), officerId, officerId);

    sendToOfficerTopic(envelope);
    awaitConsumerCommit();

    assertThat(queryRepository.findFullById(CompanyId.of(companyA)).orElseThrow().officers()).isEmpty();
    assertThat(queryRepository.findFullById(CompanyId.of(companyB)).orElseThrow().officers()).isEmpty();
}
```

- [ ] **Step 3: Run the new tests — they should fail**

```bash
mvn -pl company-service -Dit.test=OfficerEventConsumerIT verify
```

Expected: the two new tests fail because the consumer currently logs and ignores those event types.

- [ ] **Step 4: Add `findCompaniesContainingOfficer` to `CompanyQueryRepository` (port)**

Open `company-service/src/main/java/com/company/companyservice/domain/port/infrastructure/CompanyQueryRepository.java` and add:

```java
    java.util.List<CompanyFullView> findCompaniesContainingOfficer(java.util.UUID officerId);
```

(Use existing import style; place near the other `findFull*` methods.)

- [ ] **Step 5: Implement it in `MongoCompanyQueryRepository`**

In the Mongo adapter, add:

```java
    @Override
    public List<CompanyFullView> findCompaniesContainingOfficer(UUID officerId) {
        Query q = new Query(Criteria.where("officers.officerId").is(officerId));
        return mongoTemplate.find(q, CompanyDocument.class).stream()
                .map(documentMapper::toFullView)
                .toList();
    }
```

(Match imports/conventions of the surrounding code; if the existing repository uses Spring Data derived methods rather than `MongoTemplate`, prefer a derived `findByOfficers_OfficerId(UUID id)` instead. Read the file before deciding.)

Update `InMemoryCompanyQueryRepository` test stub to implement the new method by filtering its in-memory map.

- [ ] **Step 6: Extend `OfficerEventConsumer` to handle the two events**

Replace the `switch` block in `OfficerEventConsumer.onMessage` (currently lines 56–64) with:

```java
            switch (eventType) {
                case "OfficerLinkedToCompanyEvent" -> handleOfficerLinked(payload);
                case "OfficerUnlinkedFromCompanyEvent" -> handleOfficerUnlinked(payload);
                case "OfficerUpdatedEvent" -> handleOfficerUpdated(payload);
                case "OfficerDeletedEvent" -> handleOfficerDeleted(payload);
                case "OfficerCreatedEvent" ->
                        log.debug("Ignoring OfficerCreatedEvent — read model only stores officers linked to a company");
                default -> log.warn("Unknown officer event type: {}", eventType);
            }
```

Then add the two new handler methods:

```java
    private void handleOfficerUpdated(JsonNode payload) {
        UUID officerId = UUID.fromString(payload.get("aggregateId").asText());
        String firstName = payload.get("firstName").asText();
        String lastName = payload.get("lastName").asText();

        for (CompanyFullView current : queryRepository.findCompaniesContainingOfficer(officerId)) {
            List<OfficerSummary> updatedOfficers = current.officers().stream()
                    .map(o -> o.officerId().equals(officerId)
                            ? new OfficerSummary(o.officerId(), firstName, lastName, o.title())
                            : o)
                    .toList();
            queryRepository.save(rebuildWith(current, updatedOfficers));
        }
    }

    private void handleOfficerDeleted(JsonNode payload) {
        UUID officerId = UUID.fromString(payload.get("aggregateId").asText());
        for (CompanyFullView current : queryRepository.findCompaniesContainingOfficer(officerId)) {
            List<OfficerSummary> remaining = current.officers().stream()
                    .filter(o -> !o.officerId().equals(officerId))
                    .toList();
            queryRepository.save(rebuildWith(current, remaining));
        }
    }

    private static CompanyFullView rebuildWith(CompanyFullView current, List<OfficerSummary> officers) {
        return new CompanyFullView(
                current.id(),
                current.name(),
                current.registrationNumber(),
                current.address(),
                current.ownerId(),
                current.ownerDisplayName(),
                current.status(),
                current.createdAt(),
                current.updatedAt(),
                officers
        );
    }
```

You may also extract `rebuildWith` and reuse it in `handleOfficerLinked` / `handleOfficerUnlinked` for DRY (the existing code inlines this construction twice). Optional cleanup, do it if straightforward.

- [ ] **Step 7: Run the IT — it should pass**

```bash
mvn -pl company-service -Dit.test=OfficerEventConsumerIT verify
```

Expected: all officer-event consumer tests pass, including the two new ones.

- [ ] **Step 8: Commit**

```bash
git add company-service/src/main/java/com/company/companyservice/domain/port/infrastructure/CompanyQueryRepository.java \
        company-service/src/main/java/com/company/companyservice/infrastructure/persistence/query/MongoCompanyQueryRepository.java \
        company-service/src/main/java/com/company/companyservice/presentation/consumer/OfficerEventConsumer.java \
        company-service/src/test/java/com/company/companyservice/stubs/InMemoryCompanyQueryRepository.java \
        company-service/src/test/java/com/company/companyservice/integration/messaging/OfficerEventConsumerIT.java
git commit -m "feat(company-service): refresh embedded officers on OfficerUpdated and OfficerDeleted"
```

---

### Task 5: company-service — delete Feign code, ports, stubs, tests

**Files:**
- Delete: `company-service/src/main/java/com/company/companyservice/domain/port/infrastructure/OfficerQueryPort.java`
- Delete: `company-service/src/main/java/com/company/companyservice/infrastructure/feign/` (whole dir)
- Delete: `company-service/src/main/java/com/company/companyservice/config/FeignConfig.java`
- Delete: `company-service/src/test/java/com/company/companyservice/stubs/InMemoryOfficerQueryPort.java`
- Delete: `company-service/src/test/java/com/company/companyservice/integration/feign/OfficerClientIT.java`
- Delete: `company-service/src/test/java/com/company/companyservice/unit/infrastructure/feign/` (whole dir)
- Modify: `company-service/src/main/java/com/company/companyservice/CompanyServiceApplication.java`

- [ ] **Step 1: Delete the dead Feign code**

```bash
rm company-service/src/main/java/com/company/companyservice/domain/port/infrastructure/OfficerQueryPort.java
rm -r company-service/src/main/java/com/company/companyservice/infrastructure/feign
rm company-service/src/main/java/com/company/companyservice/config/FeignConfig.java
rm company-service/src/test/java/com/company/companyservice/stubs/InMemoryOfficerQueryPort.java
rm company-service/src/test/java/com/company/companyservice/integration/feign/OfficerClientIT.java
rm -r company-service/src/test/java/com/company/companyservice/unit/infrastructure/feign
rmdir company-service/src/test/java/com/company/companyservice/integration/feign 2>/dev/null || true
```

- [ ] **Step 2: Drop `@EnableFeignClients` from the application class**

Replace `CompanyServiceApplication.java`:

```java
package com.company.companyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CompanyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompanyServiceApplication.class, args);
    }
}
```

- [ ] **Step 3: Compile to confirm no dangling references**

```bash
mvn -pl company-service compile test-compile
```

Expected: clean compile. If `JwtAuthenticationFilter.CURRENT_JWT` is referenced only by the deleted `FeignConfig`, evaluate whether the field itself is still needed (it is set per request even without Feign forwarding). If unused after deletion, leave it in place — it's outside this refactor's scope.

- [ ] **Step 4: Commit**

```bash
git add -A company-service
git commit -m "refactor(company-service): remove OfficerClient and OfficerQueryPort"
```

---

### Task 6: company-service — drop Feign / Resilience4j / WireMock dependencies and config

**Files:**
- Modify: `company-service/pom.xml`
- Modify: `company-service/src/main/resources/application.yml`

- [ ] **Step 1: Remove three `<dependency>` blocks from `company-service/pom.xml`**

Delete lines 68–75 (the two Spring Cloud Feign + Resilience4j entries) and lines 160–165 (the `wiremock-jetty12` test dependency). Verify by `grep`:

```bash
grep -n "openfeign\|resilience4j\|wiremock" company-service/pom.xml
```

Expected: no matches.

- [ ] **Step 2: Remove the `feign:` and `resilience4j:` blocks from `application.yml`**

Edit `company-service/src/main/resources/application.yml`. Delete lines 22–33 (the `feign:` and `resilience4j:` blocks). The file should now have only the `spring:`, `springdoc:`, and the test profile section.

- [ ] **Step 3: Run `verify`**

```bash
mvn -pl company-service verify
```

Expected: BUILD SUCCESS. All unit + integration tests green.

- [ ] **Step 4: Commit**

```bash
git add company-service/pom.xml company-service/src/main/resources/application.yml
git commit -m "build(company-service): drop OpenFeign, Resilience4j, WireMock"
```

---

### Task 7: officer-service — create the `known_companies` projection

**Files:**
- Create: `officer-service/src/main/java/com/company/officerservice/infrastructure/persistence/query/KnownCompanyDocument.java`
- Create: `officer-service/src/main/java/com/company/officerservice/infrastructure/persistence/query/KnownCompanyMongoRepository.java`
- Create: `officer-service/src/main/java/com/company/officerservice/infrastructure/persistence/query/MongoCompanyValidationAdapter.java`

- [ ] **Step 1: Create `KnownCompanyDocument`**

```java
package com.company.officerservice.infrastructure.persistence.query;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "known_companies")
public class KnownCompanyDocument {

    @Id
    private UUID id;

    public KnownCompanyDocument() {}

    public KnownCompanyDocument(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
```

- [ ] **Step 2: Create `KnownCompanyMongoRepository`**

```java
package com.company.officerservice.infrastructure.persistence.query;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface KnownCompanyMongoRepository extends MongoRepository<KnownCompanyDocument, UUID> {
}
```

- [ ] **Step 3: Create `MongoCompanyValidationAdapter`**

```java
package com.company.officerservice.infrastructure.persistence.query;

import com.company.officerservice.domain.port.infrastructure.CompanyValidationPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MongoCompanyValidationAdapter implements CompanyValidationPort {

    private final KnownCompanyMongoRepository repository;

    public MongoCompanyValidationAdapter(KnownCompanyMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean companyExists(UUID companyId) {
        return repository.existsById(companyId);
    }
}
```

- [ ] **Step 4: Compile**

```bash
mvn -pl officer-service compile
```

Expected: success — but Spring will now have **two** `CompanyValidationPort` beans (`MongoCompanyValidationAdapter` and the existing `CompanyClientAdapter`). That will be resolved when we delete the Feign adapter in Task 9. Do not commit yet.

- [ ] **Step 5: No commit yet** — proceed to Task 8.

---

### Task 8: officer-service — extend `CompanyEventConsumer` to maintain `known_companies`

**Files:**
- Create: `officer-service/src/test/java/com/company/officerservice/integration/messaging/CompanyEventConsumerIT.java`
- Modify: `officer-service/src/main/java/com/company/officerservice/presentation/consumer/CompanyEventConsumer.java`

- [ ] **Step 1: Create `CompanyEventConsumerIT`**

Use the existing `KafkaOfficerEventPublisherIT` and `OfficerEventConsumerIT` (officer-service self-consumer test) as templates for Testcontainers Kafka + Mongo wiring. Required test cases:

```java
@Test
void companyCreatedEvent_addsCompanyToKnownCompanies() throws Exception {
    UUID companyId = UUID.randomUUID();
    String envelope = envelope("CompanyCreatedEvent", companyId, """
        {"aggregateId":"%s","name":"Acme","registrationNumber":"REG","ownerId":"%s"}
        """.formatted(companyId, UUID.randomUUID()));

    sendToCompanyTopic(envelope);
    awaitConsumerCommit();

    assertThat(knownCompanyRepository.existsById(companyId)).isTrue();
}

@Test
void companyDeletedEvent_removesCompanyFromKnownCompanies() throws Exception {
    UUID companyId = UUID.randomUUID();
    knownCompanyRepository.save(new KnownCompanyDocument(companyId));

    String envelope = envelope("CompanyDeletedEvent", companyId, """
        {"aggregateId":"%s","ownerId":"%s"}
        """.formatted(companyId, UUID.randomUUID()));

    sendToCompanyTopic(envelope);
    awaitConsumerCommit();

    assertThat(knownCompanyRepository.existsById(companyId)).isFalse();
}

@Test
void companyDeletedEvent_unlinksOfficersAndRemovesFromKnownCompanies() throws Exception {
    // Build on the existing cascade-unlink test pattern. Seed an officer linked to companyId,
    // seed companyId in known_companies, fire CompanyDeletedEvent, assert both:
    //   - officer.companyLinks no longer contains companyId
    //   - knownCompanyRepository.existsById(companyId) is false
}

@Test
void duplicateCompanyCreatedEvent_isIdempotent() throws Exception {
    UUID companyId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();

    String envelope = envelopeWithEventId(eventId, "CompanyCreatedEvent", companyId, payload);
    sendToCompanyTopic(envelope);
    sendToCompanyTopic(envelope);
    awaitConsumerCommit();

    assertThat(knownCompanyRepository.findAll()).hasSize(1);
}
```

Wire the Spring test slice so `KnownCompanyMongoRepository` is created (it will be picked up automatically by `@DataMongoTest` if the test class is in the same package or specified via `@EnableMongoRepositories`).

- [ ] **Step 2: Run the new tests — they should fail**

```bash
mvn -pl officer-service -Dit.test=CompanyEventConsumerIT verify
```

Expected: failures — the consumer doesn't handle `CompanyCreatedEvent` and doesn't touch `known_companies`.

- [ ] **Step 3: Extend the consumer**

Replace `officer-service/src/main/java/com/company/officerservice/presentation/consumer/CompanyEventConsumer.java`:

```java
package com.company.officerservice.presentation.consumer;

import com.company.officerservice.domain.model.OfficerFullView;
import com.company.officerservice.domain.port.infrastructure.OfficerCommandRepository;
import com.company.officerservice.domain.port.infrastructure.OfficerQueryRepository;
import com.company.officerservice.infrastructure.persistence.query.KnownCompanyDocument;
import com.company.officerservice.infrastructure.persistence.query.KnownCompanyMongoRepository;
import com.company.officerservice.infrastructure.persistence.query.ProcessedEventDocument;
import com.company.officerservice.infrastructure.persistence.query.ProcessedEventMongoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class CompanyEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CompanyEventConsumer.class);

    private final OfficerCommandRepository commandRepository;
    private final OfficerQueryRepository queryRepository;
    private final KnownCompanyMongoRepository knownCompanies;
    private final ProcessedEventMongoRepository processedEvents;
    private final ObjectMapper objectMapper;

    public CompanyEventConsumer(
            OfficerCommandRepository commandRepository,
            OfficerQueryRepository queryRepository,
            KnownCompanyMongoRepository knownCompanies,
            ProcessedEventMongoRepository processedEvents,
            ObjectMapper objectMapper) {
        this.commandRepository = commandRepository;
        this.queryRepository = queryRepository;
        this.knownCompanies = knownCompanies;
        this.processedEvents = processedEvents;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.company-events:company-events}",
            groupId = "officer-service-group")
    public void onMessage(String rawMessage) {
        try {
            JsonNode envelope = objectMapper.readTree(rawMessage);
            UUID eventId = UUID.fromString(envelope.get("eventId").asText());

            if (processedEvents.existsById(eventId)) {
                log.debug("Skipping already-processed company event {}", eventId);
                return;
            }

            String eventType = envelope.get("eventType").asText();
            UUID aggregateId = UUID.fromString(envelope.get("aggregateId").asText());

            switch (eventType) {
                case "CompanyCreatedEvent" -> handleCompanyCreated(aggregateId);
                case "CompanyUpdatedEvent" -> log.debug("Ignoring CompanyUpdatedEvent — known_companies stores ids only");
                case "CompanyDeletedEvent" -> handleCompanyDeleted(aggregateId);
                default -> log.debug("Ignoring company event type: {}", eventType);
            }

            processedEvents.save(new ProcessedEventDocument(eventId, Instant.now()));
        } catch (Exception e) {
            log.error("Failed to process company event", e);
            throw new RuntimeException(e);
        }
    }

    private void handleCompanyCreated(UUID companyId) {
        knownCompanies.save(new KnownCompanyDocument(companyId));
    }

    private void handleCompanyDeleted(UUID companyId) {
        knownCompanies.deleteById(companyId);

        List<OfficerFullView> linkedOfficers = queryRepository.findByCompanyId(companyId);
        for (OfficerFullView officerView : linkedOfficers) {
            commandRepository.findById(officerView.id()).ifPresent(officer -> {
                try {
                    var unlinked = officer.unlinkFromCompany(companyId);
                    commandRepository.save(unlinked.officer());
                    OfficerFullView updated = new OfficerFullView(
                            unlinked.officer().id(),
                            unlinked.officer().firstName(),
                            unlinked.officer().lastName(),
                            unlinked.officer().dateOfBirth(),
                            unlinked.officer().nationality(),
                            unlinked.officer().address(),
                            unlinked.officer().email(),
                            unlinked.officer().phone(),
                            unlinked.officer().companyLinks(),
                            unlinked.officer().createdAt(),
                            unlinked.officer().updatedAt()
                    );
                    queryRepository.save(updated);
                } catch (Exception e) {
                    log.warn("Could not unlink officer {} from deleted company {}: {}",
                            officerView.id(), companyId, e.getMessage());
                }
            });
        }
    }
}
```

- [ ] **Step 4: Run the IT — it should pass**

```bash
mvn -pl officer-service -Dit.test=CompanyEventConsumerIT verify
```

Expected: 4 tests pass (created, deleted, deleted-with-cascade, idempotent).

- [ ] **Step 5: Commit**

```bash
git add officer-service/src/main/java/com/company/officerservice/infrastructure/persistence/query/KnownCompanyDocument.java \
        officer-service/src/main/java/com/company/officerservice/infrastructure/persistence/query/KnownCompanyMongoRepository.java \
        officer-service/src/main/java/com/company/officerservice/infrastructure/persistence/query/MongoCompanyValidationAdapter.java \
        officer-service/src/main/java/com/company/officerservice/presentation/consumer/CompanyEventConsumer.java \
        officer-service/src/test/java/com/company/officerservice/integration/messaging/CompanyEventConsumerIT.java
git commit -m "feat(officer-service): maintain known_companies projection from CompanyCreated/Deleted events"
```

---

### Task 9: officer-service — delete Feign code, exception, exception handler

**Files:**
- Delete: `officer-service/src/main/java/com/company/officerservice/infrastructure/feign/` (whole dir)
- Delete: `officer-service/src/main/java/com/company/officerservice/config/FeignConfig.java`
- Delete: `officer-service/src/main/java/com/company/officerservice/domain/exception/ServiceUnavailableException.java`
- Delete: `officer-service/src/test/java/com/company/officerservice/integration/feign/CompanyClientIT.java`
- Delete: `officer-service/src/test/java/com/company/officerservice/unit/infrastructure/feign/` (whole dir)
- Modify: `officer-service/src/main/java/com/company/officerservice/presentation/controller/RestExceptionHandler.java`
- Modify: `officer-service/src/main/java/com/company/officerservice/OfficerServiceApplication.java`
- Modify: `officer-service/src/test/java/com/company/officerservice/stubs/InMemoryCompanyValidationPort.java`
- Modify: `officer-service/src/test/java/com/company/officerservice/unit/application/command/LinkOfficerToCompanyHandlerTest.java`

- [ ] **Step 1: Delete the dead Feign code**

```bash
rm -r officer-service/src/main/java/com/company/officerservice/infrastructure/feign
rm officer-service/src/main/java/com/company/officerservice/config/FeignConfig.java
rm officer-service/src/main/java/com/company/officerservice/domain/exception/ServiceUnavailableException.java
rm officer-service/src/test/java/com/company/officerservice/integration/feign/CompanyClientIT.java
rm -r officer-service/src/test/java/com/company/officerservice/unit/infrastructure/feign
rmdir officer-service/src/test/java/com/company/officerservice/integration/feign 2>/dev/null || true
```

- [ ] **Step 2: Drop the `serviceUnavailable` handler from `RestExceptionHandler`**

Open `officer-service/src/main/java/com/company/officerservice/presentation/controller/RestExceptionHandler.java`. Remove the import `import com.company.officerservice.domain.exception.ServiceUnavailableException;` and the entire `@ExceptionHandler(ServiceUnavailableException.class)` method (around lines 30–34, including the `import org.springframework.http.HttpStatus;` only if it's not used elsewhere in the file). Verify other handlers still compile.

- [ ] **Step 3: Drop `@EnableFeignClients` from the application class**

Replace `OfficerServiceApplication.java`:

```java
package com.company.officerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class OfficerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfficerServiceApplication.class, args);
    }
}
```

- [ ] **Step 4: Simplify `InMemoryCompanyValidationPort`**

Replace `officer-service/src/test/java/com/company/officerservice/stubs/InMemoryCompanyValidationPort.java`:

```java
package com.company.officerservice.stubs;

import com.company.officerservice.domain.port.infrastructure.CompanyValidationPort;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InMemoryCompanyValidationPort implements CompanyValidationPort {

    private final Set<UUID> existingCompanies = new HashSet<>();

    public void addCompany(UUID companyId) {
        existingCompanies.add(companyId);
    }

    public void clear() {
        existingCompanies.clear();
    }

    @Override
    public boolean companyExists(UUID companyId) {
        return existingCompanies.contains(companyId);
    }
}
```

- [ ] **Step 5: Drop the `ServiceUnavailableException` test from `LinkOfficerToCompanyHandlerTest`**

In `officer-service/src/test/java/com/company/officerservice/unit/application/command/LinkOfficerToCompanyHandlerTest.java`:

1. Remove the import `import com.company.officerservice.domain.exception.ServiceUnavailableException;`.
2. Delete the `linkRejected_whenCompanyServiceUnavailable` test method (lines 113–121).

The `linkRejected_whenCompanyDoesNotExist` test stays — it now exercises eventual consistency (projection not yet caught up).

- [ ] **Step 6: Run the unit + integration tests**

```bash
mvn -pl officer-service test
```

Expected: all unit tests pass. If `OfficerControllerIT` references `setSimulateUnavailable` (it shouldn't, per exploration), remove those calls.

- [ ] **Step 7: Run full `verify`**

```bash
mvn -pl officer-service verify
```

Expected: BUILD SUCCESS — all unit, integration, and IT tests green.

- [ ] **Step 8: Commit**

```bash
git add -A officer-service
git commit -m "refactor(officer-service): replace CompanyClient with MongoCompanyValidationAdapter"
```

---

### Task 10: officer-service — drop Feign / Resilience4j / WireMock dependencies and config

**Files:**
- Modify: `officer-service/pom.xml`
- Modify: `officer-service/src/main/resources/application.yml`

- [ ] **Step 1: Remove three `<dependency>` blocks from `officer-service/pom.xml`**

Delete lines 68–75 (Feign + Resilience4j) and lines 160–165 (`wiremock-jetty12`). Verify:

```bash
grep -n "openfeign\|resilience4j\|wiremock" officer-service/pom.xml
```

Expected: no matches.

- [ ] **Step 2: Remove `feign:` and `resilience4j:` blocks from `officer-service/src/main/resources/application.yml`**

Delete lines 20–33. The file should now contain only `spring:`, `springdoc:`, and the `test` profile section.

- [ ] **Step 3: Run `verify`**

```bash
mvn -pl officer-service verify
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add officer-service/pom.xml officer-service/src/main/resources/application.yml
git commit -m "build(officer-service): drop OpenFeign, Resilience4j, WireMock"
```

---

### Task 11: Update the architecture doc

**Files:**
- Modify: `docs/specs/2026-04-19-company-microservices-design.md`

- [ ] **Step 1: Locate the inter-service comm section**

```bash
grep -n -i "feign\|resilience4j\|http.*officer\|http.*company\|circuit breaker" docs/specs/2026-04-19-company-microservices-design.md
```

Read each match, decide whether the surrounding paragraph still applies. The architecture is now: every cross-service interaction uses Kafka events; each service maintains the read models / projections it needs locally.

- [ ] **Step 2: Rewrite the relevant paragraphs**

Update wording so the document reflects:
- No Feign or Resilience4j in any service.
- `company-service` serves the embedded officers list directly from `CompanyFullView`, kept in sync by `OfficerEventConsumer` (handling `OfficerLinkedToCompanyEvent`, `OfficerUnlinkedFromCompanyEvent`, `OfficerUpdatedEvent`, `OfficerDeletedEvent`).
- `officer-service` validates company existence against a `known_companies` projection maintained by `CompanyEventConsumer` (handling `CompanyCreatedEvent`, `CompanyDeletedEvent`).
- Linking to a not-yet-propagated company returns `404` and the client retries.

- [ ] **Step 3: Commit**

```bash
git add docs/specs/2026-04-19-company-microservices-design.md
git commit -m "docs: describe pure event-driven inter-service communication"
```

---

### Task 12: End-to-end verification

**Files:**
- Run: `e2e-tests/`

- [ ] **Step 1: Build everything from scratch**

```bash
mvn -pl company-service,officer-service,user-service,api-gateway,registry-service,config-service clean verify
```

Expected: BUILD SUCCESS for every module.

- [ ] **Step 2: Bring up the full stack**

```bash
docker compose up -d --build
docker compose ps
```

Expected: every service `(healthy)` once Eureka registration completes (give it ~60s).

- [ ] **Step 3: Run the e2e suite**

```bash
mvn -pl e2e-tests verify
```

Expected: all e2e scenarios pass, in particular:
- `CompanyCrudTest` — get-with-officers returns embedded officers
- `OfficerCrudTest` — link-to-company succeeds for an existing company, fails 404 for an unknown id
- `CrossServiceTest` — full event-driven workflow (create company → link officer → fetch company → delete company → officer auto-unlinked)

- [ ] **Step 4: Manual smoke (optional)**

If something looks off, drive the API by hand via the api-gateway:

```bash
# 1. Sign in as an existing user to get a JWT (use the Postman collection or raw curl).
# 2. Create a company; capture companyId.
# 3. Wait < 1s, then create an officer and link it to companyId.
#    -> Should succeed because CompanyCreatedEvent has propagated.
# 4. GET /api/companies/{companyId} -> response includes the officer in `officers`,
#    no `warnings` field anywhere on the JSON.
# 5. Delete the company.
# 6. GET /api/officers/{officerId} -> companyLinks no longer contains companyId.
```

- [ ] **Step 5: Tear down**

```bash
docker compose down -v
```

- [ ] **Step 6: No new commit** — every commit landed in earlier tasks. Confirm `git status` is clean.

---

## Verification Summary

A green run of these commands proves the refactor is complete:

```bash
# No Feign/Resilience4j references anywhere
grep -rn "FeignClient\|@EnableFeignClients\|Resilience4j\|circuitbreaker\|wiremock\|ServiceUnavailableException" \
  company-service/src officer-service/src
# Expected: zero matches

# Builds + unit + integration tests
mvn -pl company-service,officer-service verify

# Full stack
docker compose up -d --build
mvn -pl e2e-tests verify
docker compose down -v
```

If all three pass, every cross-service interaction is event-driven.
