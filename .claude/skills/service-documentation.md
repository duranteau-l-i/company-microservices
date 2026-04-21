# Service Documentation

## When to Use

When creating a new service, finishing a phase, or changing a service's public surface (endpoints, DTOs, environment variables). **Every service in this monorepo must ship with a README and an OpenAPI/Swagger spec.** Not optional.

## README (per service)

Each service has a `README.md` at its root (e.g., `user-service/README.md`). Keep it current — it is the first thing a new contributor reads.

### Required sections

1. **What this service does** — one paragraph on the bounded context, then a bullet list of responsibilities (owned aggregates, events published/consumed, cross-service calls).
2. **Architecture** — one line reaffirming DDD + Hexagonal + CQRS; link to the shared skills/specs, do not re-explain them.
3. **Endpoints** — table of method, path, public/authenticated, short description. Link to Swagger UI for detail.
4. **OpenAPI / Swagger** — URLs for Swagger UI and the raw spec. Mention the `bearerAuth` scheme when applicable.
5. **Running locally** — two modes:
   - **Dev mode**: infra in Docker (`docker compose -f docker-compose.infra.yml up -d`), service via `mvn spring-boot:run` on the host.
   - **Prod-like mode**: full stack via `docker compose up -d --build`.
6. **Configuration** — table of environment variables actually read by the service, with purpose and default. Point to `.env.example` and `config-repo/<service>.yml`.
7. **Tests** — one command per test tier:
   - `mvn test` for unit tests (InMemory pattern).
   - `mvn verify` for integration tests (Testcontainers).
   - How to run a single test.
8. **Troubleshooting** — short list (Docker not running, port collision, Flyway checksum mismatch, etc.). Add entries as real issues come up.
9. **Project layout** — ASCII tree of the service directory, one-line comments per folder.
10. **See also** — links to root `CLAUDE.md`, the design spec, and the implementation plan for this service.

### Style

- Plain Markdown, GitHub-flavored. No emojis.
- Every command must be runnable as-is from the stated working directory (service root unless noted).
- No duplication with platform-level docs — link to `CLAUDE.md` or design specs instead of repeating them.

## OpenAPI / Swagger (per service)

Every service that exposes HTTP endpoints must publish an OpenAPI spec and Swagger UI.

### Dependency

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

Pin `springdoc.version` in the service `pom.xml` properties. Use `2.6.0` or newer for Spring Boot 3.3.x.

### Bean configuration

Provide an `OpenApiConfig` under `config/`:

- Set API title, description, version on the `OpenAPI` bean.
- Declare a `bearerAuth` HTTP / bearer / JWT security scheme.
- Add `bearerAuth` as a default `SecurityRequirement` so every operation is locked unless explicitly marked public.

### Security allowlist

`SecurityConfig` must `permitAll()` the springdoc paths so Swagger UI is reachable without a token:

- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

### Controller annotations

- Put `@Tag` on each controller class — one tag per bounded capability (e.g., `Authentication`, `Users`, `Companies`).
- Put `@Operation(summary = "...")` on each endpoint — a single sentence, imperative mood, naming the actor and outcome when authorization matters ("Delete a user (ADMIN only)").
- Put `@SecurityRequirements` (empty) on controllers or operations that are public, so Swagger UI doesn't prompt for a bearer token.

### springdoc configuration

In `application.yml`:

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
```

### What the README must say

- Swagger UI: `http://localhost:<port>/swagger-ui.html`
- Raw spec: `http://localhost:<port>/v3/api-docs`
- One sentence on how to authenticate in Swagger UI (click **Authorize**, paste the JWT from `/signin`).

## Definition of Done for a service

A service is not done until:

- `README.md` exists at the service root and covers every required section.
- Swagger UI loads, lists every endpoint, and tags are grouped.
- The OpenAPI JSON validates (no springdoc warnings in the startup log).
- `docker compose up` brings the service up and Swagger UI is reachable at the mapped port.
