# Spring Security & JWT

## When to Use

When implementing authentication, authorization, JWT generation/validation, or security filters in any service.

## JWT Architecture

### Token Structure

**Access Token (30 min):**
- Header: `{ "alg": "HS256", "typ": "JWT" }`
- Payload: `{ "sub": "userId", "email": "...", "role": "USER", "iat": ..., "exp": ... }`
- Signed with shared secret from Config Service

**Refresh Token (7 days):**
- Opaque token stored hashed in PostgreSQL (user-service only)
- One-time use — rotated on each refresh

### Shared Secret

- Stored in Config Service (`application.yml`)
- All services fetch it on startup
- Same key for signing (user-service) and verification (all services + gateway)

## Auth Flow

1. `POST /api/users/signin` — user-service validates credentials, returns `{ accessToken, refreshToken, expiresIn }`
2. Client sends `Authorization: Bearer <accessToken>` on all requests
3. API Gateway validates token (signature + expiration) — rejects with 401 if invalid
4. Downstream service extracts userId and role — enforces authorization

## Implementation per Service

### JwtAuthenticationFilter

Each service (and the gateway) has a `JwtAuthenticationFilter`:
- Extends `OncePerRequestFilter`
- Extracts token from `Authorization` header
- Validates signature and expiration
- Sets `SecurityContext` with userId and role
- Skips public endpoints

### SecurityConfig

- Define public endpoints (signup, signin, refresh, actuator health)
- All other endpoints require authentication
- Stateless session management (`SessionCreationPolicy.STATELESS`)
- Add `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`

### Role Hierarchy

```
ADMIN > MANAGER > USER
```

Configure in `SecurityConfig` so `@PreAuthorize("hasRole('MANAGER')")` also grants access to ADMIN.

## Authorization Patterns

### Role-Based (Security Layer)

Use `@PreAuthorize` for role checks:
- `@PreAuthorize("hasRole('ADMIN')")` — admin only
- `@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")` — admin or manager

### Ownership-Based (Application Layer)

Ownership checks require domain context — they live in the use case handler, not the security filter:
- Load the resource, compare `ownerId` with the authenticated `userId`
- Throw a domain exception (e.g., `AccessDeniedException`) if unauthorized
- The security layer only checks "is the user authenticated and has the right role"

## Default Admin Seeding

On user-service first startup:
- Check if any ADMIN exists in the database
- If not, create one from environment variables: `ADMIN_EMAIL`, `ADMIN_PASSWORD`
- This is the only way to create an ADMIN
- Use an `ApplicationRunner` or `@PostConstruct` bean

## Password Handling

- Hash with BCrypt (Spring Security's `PasswordEncoder`)
- Never store plain text
- Compare with `passwordEncoder.matches(rawPassword, hashedPassword)`