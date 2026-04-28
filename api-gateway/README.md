# api-gateway

## What This Service Does

The API Gateway is the single entry point for all client traffic in the platform. It validates JWT access tokens at the edge, routes authenticated requests to downstream services via Eureka service discovery, and enforces CORS policy. No business logic lives here.

Responsibilities:

- JWT signature and expiration validation on every non-public request
- Route `/api/users/**` to user-service, `/api/companies/**` to company-service, `/api/officers/**` to officer-service
- CORS policy enforcement (allowedOriginPatterns: `*`)
- Aggregated Swagger UI giving clients a single view of all downstream service APIs
- No database, no Kafka — fully stateless

## Architecture

Reactive Spring Cloud Gateway (WebFlux). No DDD domain layer — the gateway is infrastructure only. See the [design spec](../docs/specs/2026-04-19-company-microservices-design.md) for platform-wide architecture decisions.

## Routes

| Method | Path prefix | Auth required | Proxied to |
|--------|-------------|---------------|------------|
| POST | `/api/users/signup` | No | user-service |
| POST | `/api/users/signin` | No | user-service |
| POST | `/api/users/auth/refresh` | No | user-service |
| * | `/api/users/**` | Yes | user-service |
| * | `/api/companies/**` | Yes | company-service |
| * | `/api/officers/**` | Yes | officer-service |
| GET | `/actuator/health` | No | gateway itself |
| GET | `/swagger-ui.html` | No | gateway itself |
| GET | `/v3/api-docs/**` | No | gateway itself |

For per-endpoint documentation, open Swagger UI for each downstream service directly (see Endpoints section of their READMEs) or use the aggregated view below.

## OpenAPI / Swagger

The gateway hosts an aggregated Swagger UI that loads the OpenAPI spec of each downstream service.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Raw spec (gateway itself, minimal): `http://localhost:8080/v3/api-docs`

On the Swagger UI page, use the dropdown in the top-right to switch between `user-service`, `company-service`, and `officer-service` specs.

To call authenticated endpoints from Swagger UI: open the target service spec, click **Authorize**, and paste the JWT access token obtained from `POST /api/users/signin`.

## Running Locally

### Dev mode (recommended)

Run infrastructure and config/registry services in Docker, gateway on the host JVM.

```bash
# From the repo root — start infra (postgres, mongo, kafka, config, registry)
docker compose -f docker-compose.infra.yml up -d

# From api-gateway/
mvn spring-boot:run
```

The gateway starts on port 8080. Downstream services (user-service etc.) must also be running on their respective host ports for Swagger URL aggregation to resolve `localhost:8081/v3/api-docs` etc.

### Prod-like mode

Build and run the full stack in Docker.

```bash
# From the repo root
docker compose up -d --build
```

Gateway is available at `http://localhost:8080`.

## Configuration

Environment variables read by this service:

| Variable | Purpose | Default |
|----------|---------|---------|
| `CONFIG_SERVER_URI` | URL of the Spring Cloud Config Server | `http://localhost:8888` |
| `SERVER_PORT` | HTTP port the gateway listens on | `8080` |
| `JWT_SECRET` | HMAC-SHA256 key for JWT validation (must match user-service) | — (required) |
| `EUREKA_URI` | Eureka server URL for service discovery | `http://localhost:8761/eureka` |

See `.env.example` at the repo root for a complete example and `config-repo/api-gateway.yml` for the full config served by the Config Server.

## Tests

Run unit tests (no Spring context, pure Java):

```bash
mvn test
```

Run integration tests (Testcontainers, requires Docker):

```bash
mvn verify
```

Run a single test class:

```bash
mvn test -Dtest=JwtAuthenticationFilterTest
```

## Troubleshooting

**Gateway starts but routes return 503**
Downstream services are not registered in Eureka yet. Check `http://localhost:8761` to confirm they are UP before sending traffic.

**JWT validation returns 401 on every request**
The `JWT_SECRET` environment variable does not match the value used by user-service to sign tokens. Both services must share the same secret (sourced from Config Server).

**Swagger UI shows "Failed to fetch" for a downstream spec**
The downstream service is not running, or the `springdoc.swagger-ui.urls` config points to the wrong host/port. In dev mode the URLs use `localhost:<port>`; in Docker they use service names.

**Port 8080 already in use**
Another process is bound to 8080. Stop it or set `SERVER_PORT=8090` before starting the gateway.

**Config Server connection refused on startup**
Start the infrastructure stack first (`docker compose -f docker-compose.infra.yml up -d`) and wait for config-service to be healthy before running the gateway.

## Project Layout

```
api-gateway/
├── pom.xml                          # Independent Maven POM, Spring Boot parent
├── Dockerfile                       # Multi-stage build (maven + jre-alpine)
└── src/
    ├── main/
    │   ├── java/com/company/apigateway/
    │   │   ├── ApiGatewayApplication.java   # Spring Boot entry point
    │   │   └── security/
    │   │       ├── JwtAuthenticationFilter.java   # WebFilter: validates JWT, sets headers
    │   │       ├── JwtTokenValidator.java          # JWT signature + expiration check
    │   │       ├── SecurityConfig.java             # WebFlux security chain
    │   │       └── SecurityProperties.java         # Binds app.security.public-paths
    │   └── resources/
    │       ├── application.yml      # Local defaults (routes, CORS, springdoc, public paths)
    │       └── bootstrap.yml        # Config Server connection
    └── test/
        └── java/com/company/apigateway/
            └── security/
                └── JwtAuthenticationFilterTest.java  # Unit tests for the JWT filter
```

## See Also

- Design specification: [docs/specs/2026-04-19-company-microservices-design.md](../docs/specs/2026-04-19-company-microservices-design.md)
- Implementation plan: [docs/plans/03-api-gateway.md](../docs/plans/03-api-gateway.md)
