# E2E Test Suite: Eventual-Consistency Retries

**Date:** 2026-07-17
**Scope:** `e2e-tests` module only — no production code changes.

## Problem

The e2e suite has intermittent failures caused by the platform's intentional CQRS eventual
consistency (see `cqrs-implementation.md`): several tests query a MongoDB read model
immediately after a write, before the async Kafka-driven sync has caught up.

Two failure clusters were observed in a full-stack run (25/39 passed, 14 failed):

1. **Officer creation right after company creation** — cross-service race. Company creation
   publishes a `company-events` message; officer-service consumes it to build its own
   `known_companies` projection, which `POST /api/officers` validates against. Creating an
   officer immediately after creating its company returns `422` until that projection catches
   up. This single race point is responsible for 12 of the 14 failures, because every one of
   those tests goes through `E2ETestBase.createOfficerForCompany`.
2. **Own-profile fetch right after signup** — same-service race. `GET /api/users/{id}` reads
   user-service's own Mongo read model, populated via an internal consumer of `user-events`.
   `UserManagementTest.userGetsOwnProfile_returns200` fetches the profile immediately after
   `signUp`, before that internal sync completes.

Both were confirmed by manual reproduction: replaying the same request sequence with a few
seconds' gap between write and read succeeds every time.

`CrossServiceTest` already works around this exact class of problem with two duplicated
hand-rolled polling loops (`awaitOfficersInCompany`, `awaitOfficerLinksDeactivated`): a
deadline + `while` + `Thread.sleep(500)` loop, written twice with no shared code.

## Approach

Extract one shared polling helper into `E2ETestBase`, operating on RestAssured's `Response` so
it covers both status-code checks and body-content checks:

```java
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
```

No new test dependency — this generalizes the pattern already present in `CrossServiceTest`
rather than introducing a library like Awaitility, keeping the suite consistent with its
existing style.

Poll interval is fixed at 500ms; timeout defaults to 10s (matches `CrossServiceTest`'s existing
default), passed explicitly at each call site so slower paths (e.g. the 15s deactivation wait)
keep their current budget.

## Changes

1. **`E2ETestBase.createOfficerForCompany`** — build the officer JSON body once (unchanged),
   then call `awaitResponse` with `ready = response.statusCode() != 422`. Once ready, assert
   `201` and extract `id` as before. This fixes all 12 downstream test failures with zero
   changes to the individual test files that call it.

2. **`UserManagementTest.userGetsOwnProfile_returns200`** — wrap the `GET
   /api/users/{userId}` call in `awaitResponse` with `ready = response.statusCode() == 200`,
   then run the existing body assertions (`id`, `email`) against the returned response.

3. **`CrossServiceTest`** — refactor `awaitOfficersInCompany` and
   `awaitOfficerLinksDeactivated` to call the new shared `awaitResponse` helper instead of
   their own duplicated loops. `awaitOfficersInCompany`'s predicate checks the parsed
   `officers` list size; `awaitOfficerLinksDeactivated`'s predicate checks that a matching,
   deactivated `companyLinks` entry exists. Both keep their current timeouts (10s and 15s
   respectively). No behavior change, ~25 lines of duplication removed.

4. **`CompanyCrudTest` hardening** — `getOwnCompany_returnsFullView`,
   `listCompanies_userSeesOwnCompanies`, and `searchCompanies_returnsMatchByName` read
   company-service's own Mongo projection immediately after `createCompany`. This is the same
   race pattern (same-service, single Kafka hop), just fast enough in practice that it didn't
   trigger a failure in the observed run. Wrap each of these three reads in `awaitResponse`
   (10s timeout, predicate on status 200 + the specific body condition each test already
   asserts) for consistency and to preempt future flakiness under load.

## Non-goals

- No production code changes. The read-after-write lag is intentional, documented platform
  behavior (`cqrs-implementation.md`); the suite should tolerate it, not eliminate it.
- No new test dependencies (ruling out Awaitility) — stays consistent with the existing
  hand-rolled style in `CrossServiceTest`.
- Not attempting to make every e2e test race-proof preemptively — only the two confirmed
  failure points plus the one same-architecture cluster in `CompanyCrudTest` called out above.
  `AuthFlowTest` and the rest of `UserManagementTest`/`CompanyCrudTest` don't read a value
  produced by an unsynced write and are left as-is.

## Verification

1. `mvn -q test` in `e2e-tests` (unit-level: none needed — this module has no unit tests, only
   e2e).
2. Full stack: `docker compose up -d --build`, wait for all containers healthy, run
   `mvn test` in `e2e-tests`, confirm all 39 tests pass, `docker compose down`.