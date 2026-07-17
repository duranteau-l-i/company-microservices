# E2E Eventual-Consistency Retries Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate the 14 intermittent e2e test failures caused by reading a CQRS Mongo read model immediately after a write, before the async Kafka-driven sync catches up, by adding a shared polling helper and applying it at every confirmed and structurally-identical race point.

**Architecture:** Add one `protected static Response awaitResponse(...)` polling helper to `E2ETestBase` (deadline + `Thread.sleep(500)` loop over a `Supplier<Response>`/`Predicate<Response>` pair, matching the hand-rolled pattern `CrossServiceTest` already uses). Apply it at the two confirmed failure points (`createOfficerForCompany`, `UserManagementTest.userGetsOwnProfile_returns200`), refactor `CrossServiceTest`'s two duplicated hand-rolled loops onto the same helper, and harden three `CompanyCrudTest` reads that share the same race shape but haven't failed yet.

**Tech Stack:** Java 21, JUnit 5, RestAssured 5.4.0 — `e2e-tests` module only. No new dependencies.

## Global Constraints

- Scope is `e2e-tests` module only — no production code (any of the six services) changes.
- No new test dependencies (no Awaitility) — extend the existing hand-rolled polling style already present in `CrossServiceTest`.
- Poll interval: fixed 500ms. Default timeout: 10s per call site, except `awaitOfficerLinksDeactivated` which keeps its existing 15s budget (passed in by its caller).
- The full Docker Compose stack must be running before executing any step that runs a test: `cd /Users/ludovicduranteau/Documents/personal/company-microservices && docker compose ps` — all 9 services must show `healthy`. If not running: `docker compose up -d --build` and wait for health.
- All commands below assume working directory `/Users/ludovicduranteau/Documents/personal/company-microservices/e2e-tests` unless stated otherwise.
- Baseline (confirmed before this plan): `mvn test` in `e2e-tests` reports `Tests run: 39, Failures: 14, Errors: 0, Skipped: 1`. The skipped test is `CrossServiceTest.officerServiceDown_getCompany_returnsWithWarning` (`@Disabled`, requires manual container stop/start) — it stays skipped throughout this plan.

---

### Task 1: Add the shared `awaitResponse` polling helper

**Files:**
- Modify: `e2e-tests/src/test/java/com/company/e2e/E2ETestBase.java`

**Interfaces:**
- Produces: `protected static Response awaitResponse(Supplier<Response> request, Predicate<Response> ready, Duration timeout, String description)` — polls `request.get()` every 500ms until `ready.test(response)` returns true or `timeout` elapses; returns the last successful `Response`; throws `AssertionError` with `description` and the last response's status/body on timeout. Inherited by every subclass of `E2ETestBase` (all five test classes) as a protected static method — callable unqualified from any test.

- [ ] **Step 1: Add imports and the helper method**

In `E2ETestBase.java`, replace the import block (current lines 1–12):

```java
package com.company.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
```

Then insert this method right after `randomString()` (current lines 92–94) and before `createCompany` (current line 96):

```java
    /**
     * Polls {@code request} every 500ms until {@code ready} accepts the response or
     * {@code timeout} elapses. Used to bridge the async Kafka-driven read-model sync
     * (see cqrs-implementation.md) between a write and a dependent read in these tests.
     */
    protected static Response awaitResponse(Supplier<Response> request, Predicate<Response> ready,
                                             Duration timeout, String description) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        Response last;
        do {
            last = request.get();
            if (ready.test(last)) {
                return last;
            }
            sleepQuietly(Duration.ofMillis(500));
        } while (System.currentTimeMillis() < deadline);
        throw new AssertionError("Timed out waiting for: " + description
                + ". Last response: " + last.statusCode() + " " + last.body().asString());
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
```

- [ ] **Step 2: Verify it compiles**

Run: `cd /Users/ludovicduranteau/Documents/personal/company-microservices/e2e-tests && mvn -q compile`
Expected: no output, exit code 0 (nothing calls `awaitResponse` yet, so this only checks the helper itself compiles).

- [ ] **Step 3: Commit**

```bash
cd /Users/ludovicduranteau/Documents/personal/company-microservices
git add e2e-tests/src/test/java/com/company/e2e/E2ETestBase.java
git commit -m "test(e2e): add shared awaitResponse polling helper"
```

---

### Task 2: Make `createOfficerForCompany` retry-safe

**Files:**
- Modify: `e2e-tests/src/test/java/com/company/e2e/E2ETestBase.java`

**Interfaces:**
- Consumes: `awaitResponse(Supplier<Response>, Predicate<Response>, Duration, String)` from Task 1.
- Produces: `protected String createOfficerForCompany(String token, String companyId)` — same signature and return contract as before (returns the created officer's id as a `String`), now internally retrying on `422` for up to 10s before asserting `201`.

This fixes the root cause behind 12 of the 14 baseline failures: `OfficerCrudTest` (10 tests, via its `@BeforeEach setupUserAndCompany` → every `@Test`) and `CrossServiceTest` (2 tests) all call this method immediately after `createCompany`, before officer-service's `known_companies` projection has synced via Kafka.

- [ ] **Step 1: Confirm the current failure**

Run: `cd /Users/ludovicduranteau/Documents/personal/company-microservices/e2e-tests && mvn test -Dtest=OfficerCrudTest#createOfficer_returns201WithId`
Expected: `FAILURE`, with `java.lang.AssertionError: 1 expectation failed. Expected status code <201> but was <422>.` at `E2ETestBase.createOfficerForCompany`. (This race is timing-dependent — if it happens to pass on this particular run, proceed to Step 2 anyway; the fix is still correct and needed for reliability.)

- [ ] **Step 2: Replace `createOfficerForCompany`**

In `E2ETestBase.java`, replace the current method (lines 117–143):

```java
    protected String createOfficerForCompany(String token, String companyId) {
        return auth(token)
                .body("""
                        {
                          "companyId": "%s",
                          "firstName": "Alice",
                          "lastName": "Smith",
                          "dateOfBirth": "1990-01-15",
                          "nationality": "French",
                          "street": "1 Rue de la Paix",
                          "city": "Paris",
                          "postalCode": "75001",
                          "country": "France",
                          "email": "%s",
                          "phone": "+33 1 23 45 67 89",
                          "title": "Director",
                          "appointmentDate": "2024-01-01"
                        }
                        """.formatted(companyId, randomEmail()))
                .when()
                .post("/api/officers")
                .then()
                .statusCode(201)
                .extract()
                .path("id")
                .toString();
    }
```

with:

```java
    protected String createOfficerForCompany(String token, String companyId) {
        String body = """
                {
                  "companyId": "%s",
                  "firstName": "Alice",
                  "lastName": "Smith",
                  "dateOfBirth": "1990-01-15",
                  "nationality": "French",
                  "street": "1 Rue de la Paix",
                  "city": "Paris",
                  "postalCode": "75001",
                  "country": "France",
                  "email": "%s",
                  "phone": "+33 1 23 45 67 89",
                  "title": "Director",
                  "appointmentDate": "2024-01-01"
                }
                """.formatted(companyId, randomEmail());

        Response response = awaitResponse(
                () -> auth(token).body(body).when().post("/api/officers"),
                r -> r.statusCode() != 422,
                Duration.ofSeconds(10),
                "officer creation for company " + companyId
                        + " (waiting for officer-service's known_companies projection to sync)");

        return response.then()
                .statusCode(201)
                .extract()
                .path("id")
                .toString();
    }
```

- [ ] **Step 3: Verify the fix**

Run: `cd /Users/ludovicduranteau/Documents/personal/company-microservices/e2e-tests && mvn test -Dtest=OfficerCrudTest,CrossServiceTest`
Expected (corrected after Task 2 execution surfaced a miscount): `Tests run: 14, Failures: 1, Errors: 0, Skipped: 1` — 10 `OfficerCrudTest` + 4 `CrossServiceTest` (3 executable + 1 `@Disabled`). One known residual failure remains: `OfficerCrudTest.linkToSecondCompany_returns200`, which was previously masked by the `createOfficerForCompany` failure this task fixes — it hits the identical race one level deeper, via `linkOfficer`. Task 2b (below) fixes it.

- [ ] **Step 4: Commit**

```bash
cd /Users/ludovicduranteau/Documents/personal/company-microservices
git add e2e-tests/src/test/java/com/company/e2e/E2ETestBase.java
git commit -m "test(e2e): retry officer creation until company projection syncs"
```

---

### Task 2b: Make `linkOfficer` retry-safe

**Files:**
- Modify: `e2e-tests/src/test/java/com/company/e2e/E2ETestBase.java`

**Interfaces:**
- Consumes: `awaitResponse(Supplier<Response>, Predicate<Response>, Duration, String)` from Task 1.
- Produces: `protected ValidatableResponse linkOfficer(String token, String officerId, String companyId)` — same signature and return type as before (a `ValidatableResponse` callers chain `.statusCode(...)` and `.body(...)` onto), now internally retrying on `422` for up to 10s before returning.

**Why this task exists:** Task 2 fixed `createOfficerForCompany`, which unmasked a second instance of the exact same race: `OfficerCrudTest.linkToSecondCompany_returns200` creates a second company and immediately calls `linkOfficer` against it — racing officer-service's `known_companies` projection sync for that second company, exactly like Task 2's race but through a different method. This was invisible in the original baseline because the test failed earlier (at `createOfficerForCompany`) every time. `linkOfficer` is also called by `OfficerCrudTest.linkDuplicate_returns409` against an already-synced company (expects an immediate `409`, not `422`) — retrying only while the response is `422` leaves that test's behavior unchanged, since `409 != 422` resolves on the first attempt.

- [ ] **Step 1: Confirm the current failure**

Run: `cd /Users/ludovicduranteau/Documents/personal/company-microservices/e2e-tests && mvn test -Dtest=OfficerCrudTest#linkToSecondCompany_returns200`
Expected: `FAILURE`, with `java.lang.AssertionError: 1 expectation failed. Expected status code <200> but was <422>.` at `OfficerCrudTest.linkToSecondCompany_returns200`, inside the `linkOfficer(...)` call.

- [ ] **Step 2: Replace `linkOfficer`**

In `E2ETestBase.java`, replace the current method:

```java
    protected ValidatableResponse linkOfficer(String token, String officerId, String companyId) {
        return auth(token)
                .body("""
                        {
                          "companyId": "%s",
                          "title": "Secretary",
                          "appointmentDate": "2024-06-01"
                        }
                        """.formatted(companyId))
                .when()
                .post("/api/officers/" + officerId + "/links")
                .then();
    }
```

with:

```java
    protected ValidatableResponse linkOfficer(String token, String officerId, String companyId) {
        String body = """
                {
                  "companyId": "%s",
                  "title": "Secretary",
                  "appointmentDate": "2024-06-01"
                }
                """.formatted(companyId);

        Response response = awaitResponse(
                () -> auth(token).body(body).when().post("/api/officers/" + officerId + "/links"),
                r -> r.statusCode() != 422,
                Duration.ofSeconds(10),
                "officer link to company " + companyId
                        + " (waiting for officer-service's known_companies projection to sync)");

        return response.then();
    }
```

- [ ] **Step 3: Verify the fix**

Run: `cd /Users/ludovicduranteau/Documents/personal/company-microservices/e2e-tests && mvn test -Dtest=OfficerCrudTest,CrossServiceTest`
Expected: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 1` — all `OfficerCrudTest` and non-disabled `CrossServiceTest` tests pass, including `linkToSecondCompany_returns200` and `linkDuplicate_returns409` (which must still return `409`, not be affected by the retry).

- [ ] **Step 4: Commit**

```bash
cd /Users/ludovicduranteau/Documents/personal/company-microservices
git add e2e-tests/src/test/java/com/company/e2e/E2ETestBase.java
git commit -m "test(e2e): retry officer-company link until second company's projection syncs"
```

---

### Task 3: Fix `UserManagementTest.userGetsOwnProfile_returns200`

**Files:**
- Modify: `e2e-tests/src/test/java/com/company/e2e/UserManagementTest.java`

**Interfaces:**
- Consumes: `awaitResponse(Supplier<Response>, Predicate<Response>, Duration, String)` from Task 1 (inherited from `E2ETestBase`).

Fixes the 14th and last baseline failure: this test reads `GET /api/users/{id}` immediately after `signUp`, before user-service's own internal Kafka consumer has synced its Mongo read model.

- [ ] **Step 1: Confirm the current failure**

Run: `cd /Users/ludovicduranteau/Documents/personal/company-microservices/e2e-tests && mvn test -Dtest=UserManagementTest#userGetsOwnProfile_returns200`
Expected: `FAILURE`, with `java.lang.AssertionError: 1 expectation failed. Expected status code <200> but was <404>.`

- [ ] **Step 2: Add imports and replace the test**

In `UserManagementTest.java`, replace the import block (current lines 1–6):

```java
package com.company.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
```

Replace the test method (current lines 96–109):

```java
    @Test
    void userGetsOwnProfile_returns200() {
        String email = randomEmail();
        String userId = signUp(email, "Password123!", "Self", "Reader");
        String token = signIn(email, "Password123!");

        auth(token)
                .when()
                .get("/api/users/" + userId)
                .then()
                .statusCode(200)
                .body("id", equalTo(userId))
                .body("email", equalTo(email));
    }
```

with:

```java
    @Test
    void userGetsOwnProfile_returns200() {
        String email = randomEmail();
        String userId = signUp(email, "Password123!", "Self", "Reader");
        String token = signIn(email, "Password123!");

        Response response = awaitResponse(
                () -> auth(token).when().get("/api/users/" + userId),
                r -> r.statusCode() == 200,
                Duration.ofSeconds(10),
                "own profile to sync for user " + userId);

        response.then()
                .statusCode(200)
                .body("id", equalTo(userId))
                .body("email", equalTo(email));
    }
```

- [ ] **Step 3: Verify the fix**

Run: `cd /Users/ludovicduranteau/Documents/personal/company-microservices/e2e-tests && mvn test -Dtest=UserManagementTest`
Expected: `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`.

- [ ] **Step 4: Commit**

```bash
cd /Users/ludovicduranteau/Documents/personal/company-microservices
git add e2e-tests/src/test/java/com/company/e2e/UserManagementTest.java
git commit -m "test(e2e): retry own-profile fetch until user read model syncs"
```

---

### Task 4: Refactor `CrossServiceTest`'s hand-rolled polling onto the shared helper

**Files:**
- Modify: `e2e-tests/src/test/java/com/company/e2e/CrossServiceTest.java`

**Interfaces:**
- Consumes: `awaitResponse(Supplier<Response>, Predicate<Response>, Duration, String)` from Task 1.
- Produces: `awaitOfficersInCompany(String token, String companyId, int minCount)` and `awaitOfficerLinksDeactivated(String token, String officerId, String companyId, int timeoutSeconds)` keep their exact current signatures and call sites — only their internals change, and both drop `throws InterruptedException` (no longer needed since `awaitResponse` doesn't declare it).

This is a pure behavior-preserving cleanup: `CrossServiceTest` already has two duplicated deadline/while/`Thread.sleep(500)` loops solving this exact problem. Both already pass at baseline (only failing indirectly today via `createOfficerForCompany`, fixed in Task 2) — this task removes the duplication now that a shared helper exists.

- [ ] **Step 1: Replace imports**

Replace the import block (current lines 1–11):

```java
package com.company.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
```

(This drops `java.util.concurrent.TimeUnit` and `static org.assertj.core.api.Assertions.assertThat`, both only used by the two loops being replaced, and adds `io.restassured.response.Response` and `java.time.Duration`.)

- [ ] **Step 2: Remove `throws InterruptedException` from the two test methods**

Change (current line 16):
```java
    void getCompanyWithLinkedOfficer_returnsOfficersFromOfficerService() throws InterruptedException {
```
to:
```java
    void getCompanyWithLinkedOfficer_returnsOfficersFromOfficerService() {
```

Change (current line 55):
```java
    void deleteCompany_deactivatesOfficerLinks() throws InterruptedException {
```
to:
```java
    void deleteCompany_deactivatesOfficerLinks() {
```

- [ ] **Step 3: Replace `awaitOfficersInCompany`**

Replace (current lines 94–113):

```java
    // Polls until the company response contains at least minCount officers.
    private void awaitOfficersInCompany(String token, String companyId, int minCount)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (System.currentTimeMillis() < deadline) {
            List<?> officers = given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/companies/" + companyId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("officers");
            if (officers != null && officers.size() >= minCount) {
                return;
            }
            Thread.sleep(500);
        }
        assertThat(false).as("Timed out waiting for officers to appear in company %s", companyId).isTrue();
    }
```

with:

```java
    // Polls until the company response contains at least minCount officers.
    private void awaitOfficersInCompany(String token, String companyId, int minCount) {
        awaitResponse(
                () -> given().header("Authorization", "Bearer " + token)
                        .when()
                        .get("/api/companies/" + companyId),
                r -> {
                    List<?> officers = r.then().statusCode(200).extract().path("officers");
                    return officers != null && officers.size() >= minCount;
                },
                Duration.ofSeconds(10),
                "officers to appear in company " + companyId);
    }
```

- [ ] **Step 4: Replace `awaitOfficerLinksDeactivated`**

Replace (current lines 115–142):

```java
    // Polls until every officer link to the given company is deactivated (active=false).
    private void awaitOfficerLinksDeactivated(String token, String officerId, String companyId, int timeoutSeconds)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        while (System.currentTimeMillis() < deadline) {
            List<java.util.Map<String, Object>> links = given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/officers/" + officerId + "/companies")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("companyLinks");
            if (links != null) {
                boolean allDeactivated = links.stream()
                        .filter(l -> companyId.equals(String.valueOf(l.get("companyId"))))
                        .allMatch(l -> Boolean.FALSE.equals(l.get("active")));
                boolean hasMatch = links.stream()
                        .anyMatch(l -> companyId.equals(String.valueOf(l.get("companyId"))));
                if (hasMatch && allDeactivated) {
                    return;
                }
            }
            Thread.sleep(500);
        }
        assertThat(false).as("Timed out waiting for officer %s links to company %s to be deactivated",
                officerId, companyId).isTrue();
    }
```

with:

```java
    // Polls until every officer link to the given company is deactivated (active=false).
    private void awaitOfficerLinksDeactivated(String token, String officerId, String companyId, int timeoutSeconds) {
        awaitResponse(
                () -> given().header("Authorization", "Bearer " + token)
                        .when()
                        .get("/api/officers/" + officerId + "/companies"),
                r -> {
                    List<java.util.Map<String, Object>> links = r.then().statusCode(200).extract().path("companyLinks");
                    if (links == null) {
                        return false;
                    }
                    boolean hasMatch = links.stream()
                            .anyMatch(l -> companyId.equals(String.valueOf(l.get("companyId"))));
                    boolean allDeactivated = links.stream()
                            .filter(l -> companyId.equals(String.valueOf(l.get("companyId"))))
                            .allMatch(l -> Boolean.FALSE.equals(l.get("active")));
                    return hasMatch && allDeactivated;
                },
                Duration.ofSeconds(timeoutSeconds),
                "officer " + officerId + " links to company " + companyId + " to be deactivated");
    }
```

- [ ] **Step 5: Verify no regression**

Run: `cd /Users/ludovicduranteau/Documents/personal/company-microservices/e2e-tests && mvn test -Dtest=CrossServiceTest`
Expected: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 1`.

- [ ] **Step 6: Commit**

```bash
cd /Users/ludovicduranteau/Documents/personal/company-microservices
git add e2e-tests/src/test/java/com/company/e2e/CrossServiceTest.java
git commit -m "test(e2e): dedupe CrossServiceTest polling onto shared awaitResponse helper"
```

---

### Task 5: Harden `CompanyCrudTest`'s read-after-write tests

**Files:**
- Modify: `e2e-tests/src/test/java/com/company/e2e/CompanyCrudTest.java`

**Interfaces:**
- Consumes: `awaitResponse(Supplier<Response>, Predicate<Response>, Duration, String)` from Task 1.

`getOwnCompany_returnsFullView`, `listCompanies_userSeesOwnCompanies`, and `searchCompanies_returnsMatchByName` read company-service's own Mongo projection immediately after `createCompany` — the same race shape as Tasks 2–3 (same-service, single Kafka hop), just fast enough in practice that it hasn't failed in observed runs. This task preempts future flakiness; it is not fixing a currently-broken test, so there is no "confirm the failure first" step — just a before/after regression check.

- [ ] **Step 1: Add imports**

Replace the import block (current lines 1–7):

```java
package com.company.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
```

- [ ] **Step 2: Harden `getOwnCompany_returnsFullView`**

Replace (current lines 38–53):

```java
    @Test
    void getOwnCompany_returnsFullView() {
        String email = randomEmail();
        String ownerId = signUp(email, "Password123!", "Owner", "A");
        String token = signIn(email, "Password123!");
        String companyId = createCompany(token, "MyCompany " + randomString(), "REG-" + randomString());

        auth(token)
                .when()
                .get("/api/companies/" + companyId)
                .then()
                .statusCode(200)
                .body("id", equalTo(companyId))
                .body("ownerId", equalTo(ownerId))
                .body("registrationNumber", notNullValue());
    }
```

with:

```java
    @Test
    void getOwnCompany_returnsFullView() {
        String email = randomEmail();
        String ownerId = signUp(email, "Password123!", "Owner", "A");
        String token = signIn(email, "Password123!");
        String companyId = createCompany(token, "MyCompany " + randomString(), "REG-" + randomString());

        Response response = awaitResponse(
                () -> auth(token).when().get("/api/companies/" + companyId),
                r -> r.statusCode() == 200,
                Duration.ofSeconds(10),
                "company " + companyId + " to appear in the read model");

        response.then()
                .statusCode(200)
                .body("id", equalTo(companyId))
                .body("ownerId", equalTo(ownerId))
                .body("registrationNumber", notNullValue());
    }
```

- [ ] **Step 3: Harden `listCompanies_userSeesOwnCompanies`**

Replace (current lines 202–215):

```java
    @Test
    void listCompanies_userSeesOwnCompanies() {
        String email = randomEmail();
        signUp(email, "Password123!", "Lister", "User");
        String token = signIn(email, "Password123!");
        createCompany(token, "Listed " + randomString(), "REG-" + randomString());

        auth(token)
                .when()
                .get("/api/companies")
                .then()
                .statusCode(200)
                .body("$", not(empty()));
    }
```

with:

```java
    @Test
    void listCompanies_userSeesOwnCompanies() {
        String email = randomEmail();
        signUp(email, "Password123!", "Lister", "User");
        String token = signIn(email, "Password123!");
        createCompany(token, "Listed " + randomString(), "REG-" + randomString());

        Response response = awaitResponse(
                () -> auth(token).when().get("/api/companies"),
                r -> {
                    List<?> companies = r.then().statusCode(200).extract().path("$");
                    return companies != null && !companies.isEmpty();
                },
                Duration.ofSeconds(10),
                "user's company list to include the newly created company");

        response.then()
                .statusCode(200)
                .body("$", not(empty()));
    }
```

- [ ] **Step 4: Harden `searchCompanies_returnsMatchByName`**

Replace (current lines 217–232):

```java
    @Test
    void searchCompanies_returnsMatchByName() {
        String ownerToken = signUpAndSignIn(randomEmail(), "Password123!");
        String uniqueName = "SearchTarget-" + randomString();
        createCompany(ownerToken, uniqueName, "REG-" + randomString());

        String otherToken = signUpAndSignIn(randomEmail(), "Password123!");

        auth(otherToken)
                .queryParam("term", uniqueName)
                .when()
                .get("/api/companies/search")
                .then()
                .statusCode(200)
                .body("[0].name", equalTo(uniqueName));
    }
```

with:

```java
    @Test
    void searchCompanies_returnsMatchByName() {
        String ownerToken = signUpAndSignIn(randomEmail(), "Password123!");
        String uniqueName = "SearchTarget-" + randomString();
        createCompany(ownerToken, uniqueName, "REG-" + randomString());

        String otherToken = signUpAndSignIn(randomEmail(), "Password123!");

        Response response = awaitResponse(
                () -> auth(otherToken).queryParam("term", uniqueName).when().get("/api/companies/search"),
                r -> {
                    List<?> matches = r.then().statusCode(200).extract().path("$");
                    return matches != null && !matches.isEmpty();
                },
                Duration.ofSeconds(10),
                "search results for company " + uniqueName + " to appear");

        response.then()
                .statusCode(200)
                .body("[0].name", equalTo(uniqueName));
    }
```

- [ ] **Step 5: Verify no regression**

Run: `cd /Users/ludovicduranteau/Documents/personal/company-microservices/e2e-tests && mvn test -Dtest=CompanyCrudTest`
Expected: `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`.

- [ ] **Step 6: Commit**

```bash
cd /Users/ludovicduranteau/Documents/personal/company-microservices
git add e2e-tests/src/test/java/com/company/e2e/CompanyCrudTest.java
git commit -m "test(e2e): harden CompanyCrudTest reads against same read-model race"
```

---

### Task 6: Full-suite validation

**Files:** none (verification only).

- [ ] **Step 1: Run the entire e2e suite**

Run: `cd /Users/ludovicduranteau/Documents/personal/company-microservices/e2e-tests && mvn test`
Expected: `Tests run: 39, Failures: 0, Errors: 0, Skipped: 1` (the single skip remains `CrossServiceTest.officerServiceDown_getCompany_returnsWithWarning`, `@Disabled` by design).

- [ ] **Step 2: Spot-check for remaining flakiness**

Run the suite two more times to confirm the fix isn't itself timing-lucky:

`cd /Users/ludovicduranteau/Documents/personal/company-microservices/e2e-tests && mvn test && mvn test`

Expected: both runs report `Tests run: 39, Failures: 0, Errors: 0, Skipped: 1`.

- [ ] **Step 3: Tear down the stack (if it was started solely for this work)**

Run: `cd /Users/ludovicduranteau/Documents/personal/company-microservices && docker compose down`

(Skip this step if the user wants the stack left running for further work — check before tearing down.)