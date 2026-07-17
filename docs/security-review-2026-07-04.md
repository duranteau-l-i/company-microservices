
# Security Review — Company Microservices Platform
**Date:** 2026-07-04
**Branch reviewed:** `events-driven` (full project, not diff-only)
**Scope:** user-service, api-gateway, company-service, officer-service, config-service, registry-service, config-repo, Docker Compose

---

## Methodology

Three parallel identification passes covered the whole codebase: user-service + api-gateway, company-service + officer-service, and config-service/registry-service/config-repo/Docker Compose. Seven candidate findings then went through independent false-positive filtering. Two survived as real issues; five were filtered as hardening items or false positives.

---

## Confirmed Findings

### Vuln 1 — Default admin credentials `admin123` in committed config

**File:** `config-repo/user-service.yml:29`
**Severity:** High
**Category:** `default_credentials` / `auth_bypass`
**Confidence:** 7/10

#### Description

`config-repo/user-service.yml` sets:

```yaml
app:
  admin:
    email: ${ADMIN_EMAIL:admin@company.com}
    password: ${ADMIN_PASSWORD:admin123}
```

`AdminSeeder`'s own local default is blank and it skips seeding when the value is blank — but the config server supplies the resolved property, so when the `ADMIN_PASSWORD` environment variable is **truly absent**, the value becomes `admin123` and the guard never fires. The seeder then creates a real `ADMIN` account (`admin@company.com` / `admin123`) with a BCrypt-hashed password, and the public `/api/users/signin` endpoint authenticates it.

#### Exploit scenario

Any deployment (or dev run via `mvn spring-boot:run`) that does not explicitly set `ADMIN_PASSWORD` boots with a well-known ADMIN login. An attacker calls `POST /api/users/signin` with `admin@company.com` / `admin123`, receives an ADMIN access token, and gains full platform control (create/delete users, role escalation, read all users).

#### Important nuance

The full-stack `docker-compose.yml` path passes `ADMIN_PASSWORD: ${ADMIN_PASSWORD}`, which resolves to an **empty string** when the host var is unset — the empty value trips the intended skip-guard, so seeding is skipped there. The `admin123` fallback only becomes active when the variable is *entirely absent* (host dev mode, or an orchestrator that omits rather than empties the var). Real, but narrower than a blanket "always seeds admin123."

#### Fix

Remove the committed fallback — change to `password: ${ADMIN_PASSWORD}` (no default) and `email: ${ADMIN_EMAIL}`. With no default, an unset variable resolves to blank and the seeder correctly skips. Optionally add a fail-fast in a non-dev profile when no admin password is configured.

---

### Vuln 2 — Unauthenticated Eureka registry enables service impersonation → signin credential interception

**Files:** `registry-service/` (no security), `docker-compose.infra.yml` (port `8761:8761` bound `0.0.0.0`)
**Severity:** Medium
**Category:** `unauthenticated_endpoint`
**Confidence:** 7/10

#### Description

`registry-service` has no Spring Security on the classpath, so the Eureka REST API (`POST /eureka/apps/{appName}`) is fully unauthenticated. `docker-compose.infra.yml` publishes `8761:8761` bound to `0.0.0.0`. The gateway routes `/api/users/**` (including public `/api/users/signin`) via `uri: lb://user-service`, which resolves instances through Eureka. Self-preservation is disabled (`enable-self-preservation: false`), but that does not defend — a rogue instance that keeps heartbeating stays registered indefinitely.

#### Exploit scenario

An attacker who can reach port 8761 registers a second `user-service` instance pointing at their own host. The legitimate instance remains registered, so Spring Cloud LoadBalancer round-robins — roughly half of all signin requests (carrying plaintext email + password in the body) are steered to the attacker on a sustained basis. The attacker proxies to the real service to stay stealthy.

#### Fix

Bind the published port to loopback (`127.0.0.1:8761:8761`) and/or add authentication (basic or mTLS) to Eureka with registering clients authenticating. Do not expose 8761 to untrusted networks.

---

## Filtered Candidates (not exploitable in current state)

| Candidate | Verdict | Why |
|---|---|---|
| Config Server unauthenticated on port 8888 | Not a secret exposure | The config-service container holds no real secrets (only `CONFIG_REPO_LOCATION`); fetching returns only committed dev defaults + topology. Collapses into Vuln 1. |
| DB/Kafka/ZK ports published to host | Hardening only | Dev-only infra file; Postgres/Mongo are authenticated; real creds live in gitignored `.env`, not committed. |
| Wildcard CORS + `allowCredentials: true` | Inert today | Auth is bearer-token in the `Authorization` header, no cookies — nothing for a cross-origin page to abuse. Becomes real only if cookie auth is ever added. |
| Unauthenticated gateway `/actuator/metrics` | No secret/PII exposed | Exposes only operational metrics; `info.env.enabled` populates `info.*` props, not env vars (the unexposed `env` endpoint does that). |
| Officer search missing USER role scoping | Intended design | Response is `OfficerRestrictedView` (id, name, company links only — no DOB/PII); the spec deliberately grants USER restricted search including DOB-as-filter for deduplication. |

---

## Mitigation Plan

### Priority 1 — Remove default admin credentials (Vuln 1)

**File:** `config-repo/user-service.yml`

```yaml
# before
app:
  admin:
    email: ${ADMIN_EMAIL:admin@company.com}
    password: ${ADMIN_PASSWORD:admin123}

# after
app:
  admin:
    email: ${ADMIN_EMAIL}
    password: ${ADMIN_PASSWORD}
```

Additional steps:
- Verify `AdminSeeder` treats blank as "skip seeding" (it does today) and logs a clear warning.
- Optional hardening: in a `prod` profile, fail startup if `ADMIN_PASSWORD` is blank, so a real deployment cannot silently run without a configured admin.
- Confirm `.env.example` continues to carry a `change-me` placeholder (it does).

---

### Priority 2 — Lock down infrastructure port exposure (Vuln 2 + defense-in-depth)

**File:** `docker-compose.infra.yml` — bind management/infra ports to loopback:

```yaml
registry-service:
  ports: ["127.0.0.1:8761:8761"]

config-service:
  ports: ["127.0.0.1:8888:8888"]

postgres:
  ports: ["127.0.0.1:5432:5432"]

mongodb:
  ports: ["127.0.0.1:27017:27017"]

kafka:
  ports: ["127.0.0.1:9092:9092", "127.0.0.1:29092:29092"]
```

Additional steps:
- For anything beyond local dev, add authentication to Eureka and the config server (`spring-boot-starter-security` + basic auth or mTLS) and have clients send credentials.
- Keep DB/Kafka ports off the host entirely in any shared/prod compose.

---

### Priority 3 — Hardening backlog (lower urgency, not exploitable today)

- **CORS:** Replace `allowedOriginPatterns: "*"` with an explicit trusted-origin allowlist in `config-repo/api-gateway.yml`, or set `allowCredentials: false`. Implement before introducing any cookie/session auth.
- **Gateway actuator:** Trim `management.endpoints.web.exposure.include` to `health` on the gateway in `config-repo/api-gateway.yml`, or move actuator to an internal-only management port.
- **Kafka:** If Kafka (`29092`) is ever exposed outside the Docker network in a real deployment, enable SASL authentication and TLS.

---

## Areas Reviewed and Found Clean

- **JWT validation** (all services + gateway): signature and expiration verified via jjwt `verifyWith().parseSignedClaims()`; no `none`-alg or unsigned-parse path; `type=access` enforcement; secret requires ≥32 chars with no insecure fallback.
- **Downstream header trust**: downstream services independently validate JWT tokens; they do not trust gateway-injected `X-User-Id`/`X-User-Role` headers, so gateway bypass does not yield auth escalation.
- **Refresh tokens**: 256-bit `SecureRandom`, stored as SHA-256 hash only, one-time use (deleted before re-issue), expiry enforced, revoked on user delete.
- **Passwords**: BCrypt everywhere; never logged; absent from MongoDB read models and REST responses.
- **Mass assignment**: `SignUpRequest` has no role field (forced to USER); `CreateUserRequest.role` gated by `Role.canCreate`; delete is ADMIN-only.
- **SQL injection**: all native queries (including outbox `FOR UPDATE SKIP LOCKED`) use bound parameters — no string concatenation.
- **NoSQL injection**: `OfficerQueryRepositoryAdapter` wraps user input with `Pattern.quote()` before passing to MongoDB `.regex()`; company search uses derived queries only.
- **Kafka deserialization**: consumers use `objectMapper.readTree()` + `treeToValue()` into concrete classes; no polymorphic default typing; `ADD_TYPE_INFO_HEADERS: false`. Not exploitable for RCE.
- **Outbox pattern** (branch changes): purely server-side data flow; no new user-input surface.
- **Authorization completeness**: all command handlers enforce role + ownership consistently with the spec matrix. Query handlers downgrade to restricted views for non-privileged callers.
