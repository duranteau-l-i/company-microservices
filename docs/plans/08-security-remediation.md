# Security Remediation Plan

Findings from the full-codebase security review. Each item includes the exact file(s) to change, the concrete fix to apply, and the acceptance criteria.

---

## VULN-1 — IDOR: Any user can read any user profile

**Severity:** HIGH  
**Service:** user-service  
**File:** `user-service/src/main/java/com/company/userservice/application/query/GetUserHandler.java`

### Problem

`GetUserHandler.get()` ignores the `callerId` and `callerRole` fields on the `Query` record. Any authenticated user can fetch the full `UserReadModel` of any other user by UUID.

### Fix

In `GetUserHandler.get()`, enforce one of two conditions before returning data:

1. The caller is `ADMIN` or `MANAGER`, **or**
2. The `callerId` matches the requested `userId`.

Throw a `UserNotFoundException` (not an authorization error — do not leak existence) when neither condition holds and the caller is `USER` role.

```java
// GetUserHandler.java
boolean isSelf = command.callerId().equals(command.userId());
boolean isPrivileged = command.callerRole() == Role.ADMIN || command.callerRole() == Role.MANAGER;
if (!isSelf && !isPrivileged) {
    throw new UserNotFoundException(command.userId());
}
```

### Acceptance Criteria

- Unit test: USER calling `get()` with a different `userId` throws `UserNotFoundException`.
- Unit test: USER calling `get()` with their own `userId` returns the profile.
- Unit test: MANAGER calling `get()` with any `userId` returns the profile.

---

## VULN-2 — Privilege Escalation: MANAGER can update ADMIN accounts

**Severity:** HIGH  
**Service:** user-service  
**File:** `user-service/src/main/java/com/company/userservice/application/command/UpdateUserHandler.java`

### Problem

The privileged check grants `MANAGER` and `ADMIN` identical write access on any user. A MANAGER can modify the profile of any `ADMIN` account.

### Fix

After loading the target user aggregate, add a role hierarchy check:

```java
// UpdateUserHandler.java — after loading targetUser
boolean callerIsAdmin = command.callerRole() == Role.ADMIN;
boolean targetIsAdmin = targetUser.getRole() == Role.ADMIN;
boolean targetIsManager = targetUser.getRole() == Role.MANAGER;

if (command.callerRole() == Role.MANAGER && (targetIsAdmin || (targetIsManager && !isSelf))) {
    throw new InsufficientPrivilegesException("MANAGER cannot update ADMIN or other MANAGER accounts");
}
```

Rule: Only `ADMIN` may update other `ADMIN` or `MANAGER` accounts. A `MANAGER` may only update `USER` accounts or themselves.

### Acceptance Criteria

- Unit test: MANAGER targeting ADMIN account throws `InsufficientPrivilegesException`.
- Unit test: MANAGER targeting another MANAGER account throws `InsufficientPrivilegesException`.
- Unit test: MANAGER targeting a USER account succeeds.
- Unit test: ADMIN targeting any account succeeds.

---

## VULN-3 — Broken Ownership Check: attacker-controlled `companyOwnerId`

**Severity:** HIGH  
**Service:** officer-service  
**Files:**
- `officer-service/.../application/command/CreateOfficerHandler.java`
- `officer-service/.../application/command/LinkOfficerToCompanyHandler.java`
- `officer-service/.../application/command/UnlinkOfficerFromCompanyHandler.java`
- `officer-service/.../presentation/controller/OfficerController.java`

### Problem

The `USER`-role ownership check compares `callerId` against `companyOwnerId` supplied in the request body or query parameter. The officer-service never validates this against the real stored owner. Any user can pass their own UUID as `companyOwnerId` to bypass the check.

### Fix

1. **Remove `companyOwnerId` from all inbound DTOs and request parameters.** It is untrusted input.
2. The officer-service already maintains a `KnownCompany` projection (MongoDB) that stores `ownerId` consumed from company events. Use it.
3. In each command handler, when `callerRole == USER`, look up the `KnownCompany` by `companyId` and assert `knownCompany.ownerId().equals(callerId)`.

```java
// Example — CreateOfficerHandler.java
if (command.callerRole() == Role.USER) {
    KnownCompany company = knownCompanyRepository.findById(command.companyId())
        .orElseThrow(() -> new CompanyNotFoundException(command.companyId()));
    if (!company.ownerId().equals(command.callerId())) {
        throw new InsufficientPrivilegesException("Caller does not own this company");
    }
}
```

Apply the identical pattern in `LinkOfficerToCompanyHandler` and `UnlinkOfficerFromCompanyHandler`.

Remove `@RequestParam UUID companyOwnerId` from `OfficerController` unlink endpoint and from any `CreateOfficerRequest` / `LinkOfficerRequest` DTO.

### Acceptance Criteria

- Unit test: USER with non-matching `callerId` throws `InsufficientPrivilegesException` (verify via InMemory `KnownCompanyRepository`).
- Unit test: USER who is the actual owner (ownerId matches) succeeds.
- Unit test: MANAGER and ADMIN bypass the ownership check.
- `companyOwnerId` no longer appears in any request DTO or controller parameter.

---

## VULN-4 — NoSQL Injection via unescaped regex (officer-service)

**Severity:** MEDIUM  
**Service:** officer-service  
**File:** `officer-service/.../infrastructure/persistence/query/OfficerQueryRepositoryAdapter.java` (lines ~49–52)

### Problem

User-supplied `firstName` and `lastName` are passed directly as PCRE regex patterns into MongoDB `$regex`. Any authenticated user can supply `.*` to enumerate all officers.

### Fix

Escape the user input as a literal string before constructing the regex criterion:

```java
import java.util.regex.Pattern;

// In the search method:
if (firstName != null && !firstName.isBlank()) {
    String escaped = Pattern.quote(firstName.trim());
    criteria = criteria.and("firstName").regex(escaped, "i");
}
if (lastName != null && !lastName.isBlank()) {
    String escaped = Pattern.quote(lastName.trim());
    criteria = criteria.and("lastName").regex(escaped, "i");
}
```

`Pattern.quote()` wraps the value in `\Q...\E`, turning it into a literal substring match.

### Acceptance Criteria

- Unit test: search with `firstName = ".*"` matches only an officer literally named `.*`, not all documents.
- Integration test: search with a normal name returns the expected subset.

---

## VULN-5 — Hardcoded / Committed JWT Secret

**Severity:** HIGH  
**Services:** all services + api-gateway  
**Files:**
- `user-service/.../security/JwtTokenProvider.java:29`
- `company-service/.../security/JwtTokenValidator.java:18`
- `officer-service/.../security/JwtTokenValidator.java:18`
- `api-gateway/.../security/JwtTokenValidator.java:20`
- `.env` (committed to repository)

### Problem

Two compounding issues:
1. Each `@Value` annotation has a weak, publicly known default secret as a fallback.
2. `.env` is committed to the repository and contains the `JWT_SECRET` used for the default deployment.

An attacker with repo read access can forge a JWT as any user with any role.

### Fix

**Step 1 — Remove all default values from `@Value`:**

```java
// Before
@Value("${jwt.secret:change-me-change-me-change-me-change-me-64-bytes}")
private String secret;

// After — fail fast if the property is missing
@Value("${jwt.secret}")
private String secret;
```

All four services need this change. Add an `@PostConstruct` validation in each to assert the secret is at least 32 characters and not a known weak default:

```java
@PostConstruct
void validateSecret() {
    if (secret == null || secret.length() < 32) {
        throw new IllegalStateException("jwt.secret must be at least 32 characters");
    }
}
```

**Step 2 — Remove `.env` from git history:**

```bash
git rm --cached .env
echo ".env" >> .gitignore
```

Then create `.env.example` with placeholder values and no real secrets. Rotate all secrets that were committed (JWT_SECRET, POSTGRES_PASSWORD, MONGO_INITDB_ROOT_PASSWORD, ADMIN_PASSWORD).

### Acceptance Criteria

- Service fails to start with a clear error if `jwt.secret` is not provided.
- `.env` is in `.gitignore`.
- `.env.example` exists with placeholder values.
- No default secret string appears in any `@Value` annotation.

---

## VULN-6 — Refresh Token Not Revocable

**Severity:** HIGH  
**Service:** user-service  
**Files:**
- `user-service/.../application/command/RefreshTokenHandler.java`
- `user-service/.../security/JwtTokenProvider.java` (lines ~65–78)
- `user-service/.../domain/` (new `RefreshToken` entity needed)

### Problem

Refresh tokens are stateless JWTs. There is no server-side record, no revocation on sign-out or account deactivation, and no one-time-use enforcement. A stolen refresh token is valid for 7 days regardless of account state.

### Fix

Implement the architecture-documented design: opaque token stored hashed in PostgreSQL.

**Step 1 — Domain changes:**

Add a `RefreshToken` entity and `RefreshTokenRepository` port in `user-service/domain/`.

```java
public record RefreshToken(RefreshTokenId id, UserId userId, String tokenHash, Instant expiresAt) {
    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
}
```

**Step 2 — Replace JWT refresh token with opaque token:**

In `JwtTokenProvider`, change `generateTokens()` to:
- Generate a cryptographically random opaque token for the refresh token (`SecureRandom` + Base64 URL-safe, 32 bytes minimum).
- Hash it with SHA-256 before storing.
- Return the raw token to the caller, store only the hash.

```java
byte[] raw = new byte[32];
new SecureRandom().nextBytes(raw);
String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
String hash = hashSha256(rawToken);
// persist RefreshToken(userId, hash, expiresAt)
return rawToken; // returned to client, never stored
```

**Step 3 — Validate on refresh:**

In `RefreshTokenHandler`, hash the incoming token and look it up by hash. Verify it is not expired and belongs to an active user. Delete the old record and store a new one (token rotation).

**Step 4 — Revoke on sign-out and deactivation:**

Add a `SignOutUseCase` that deletes all refresh tokens for the user. In `DeleteUserHandler` (or wherever deactivation happens), also delete all refresh tokens for that user.

**Step 5 — Flyway migration:**

```sql
-- V5__create_refresh_tokens_table.sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

### Acceptance Criteria

- Unit test: refresh with a valid token returns a new access token and rotates the refresh token.
- Unit test: refresh with an already-used token fails.
- Unit test: refresh after account deactivation fails.
- Integration test: sign out then refresh returns 401.
- No JWT is generated for the refresh token flow.

---

## VULN-7 — PII Leakage in `ListOfficersByCompany` and `ListCompaniesByOfficer`

**Severity:** MEDIUM  
**Service:** officer-service  
**Files:**
- `officer-service/.../application/query/ListOfficersByCompanyHandler.java`
- `officer-service/.../application/query/ListCompaniesByOfficerHandler.java`
- `officer-service/.../presentation/controller/OfficerController.java` (lines ~143–158)

### Problem

Both handlers return `OfficerFullView` (DOB, nationality, address, personal email, phone) to any authenticated user regardless of role. `GetOfficerHandler` correctly applies a `RestrictedView` for `USER`-role callers; these two handlers do not.

### Fix

Apply the same view-selection pattern as `GetOfficerHandler` in both handlers:

```java
// ListOfficersByCompanyHandler.java
public List<Object> list(Query command) {
    List<OfficerDocument> docs = queryRepository.findByCompanyId(command.companyId());
    if (command.callerRole() == Role.USER) {
        return docs.stream().map(OfficerMapper::toRestrictedView).toList();
    }
    return docs.stream().map(OfficerMapper::toFullView).toList();
}
```

Apply the identical branching in `ListCompaniesByOfficerHandler`.

For `USER`-role callers on `ListOfficersByCompany`, also enforce an ownership check: the caller must own the company (look up `KnownCompany.ownerId` as described in VULN-3).

### Acceptance Criteria

- Unit test: USER calling `listByCompany` receives `OfficerRestrictedView` objects (no DOB, address, etc.).
- Unit test: MANAGER calling `listByCompany` receives `OfficerFullView` objects.
- Unit test: USER calling `listCompaniesByOfficer` receives restricted view.
- Integration test: `GET /api/officers/by-company/{id}` as USER returns response without `dateOfBirth`, `address`, `personalEmail`.

---

## VULN-8 — Infrastructure Topology Exposed via Unauthenticated Actuator

**Severity:** MEDIUM  
**All services**  
**File:** `config-repo/application.yml` (lines ~7–8)

### Problem

`management.endpoint.health.show-details: always` causes the `/actuator/health` endpoint — which is `permitAll()` in every service's `SecurityConfig` — to return detailed component information including internal hostnames, Kafka broker addresses, database names, and connection pool state to unauthenticated callers.

### Fix

Change the shared config to only show details to authenticated users:

```yaml
# config-repo/application.yml
management:
  endpoint:
    health:
      show-details: when-authorized
  endpoints:
    web:
      exposure:
        include: health,info
```

This retains the public `UP`/`DOWN` status (needed for Docker health checks) while hiding component details from unauthenticated callers. Docker Compose health checks use the exit code, not the response body, so this is a safe change.

### Acceptance Criteria

- Unauthenticated `GET /actuator/health` returns `{ "status": "UP" }` with no component details.
- Docker Compose health checks still pass (`condition: service_healthy`).
