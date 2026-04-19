# Phase 3: API Gateway

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Spring Cloud Gateway that acts as the single entry point — validates JWT, routes to services, handles CORS.

**Architecture:** Reactive gateway (Spring WebFlux). JWT validation at the edge, routing by path prefix, no authorization logic (delegated to services).

**Tech Stack:** Spring Cloud Gateway, Spring Security (reactive), jjwt, Spring Cloud Netflix Eureka Client

---

### Task 1: Gateway Project Setup

**Files:**
- Create: `api-gateway/pom.xml`
- Create: `api-gateway/src/main/java/com/company/apigateway/ApiGatewayApplication.java`
- Create: `api-gateway/src/main/resources/application.yml`
- Create: `api-gateway/src/main/resources/bootstrap.yml`

- [ ] **Step 1: Create Maven project**
  Dependencies: `spring-cloud-starter-gateway`, `spring-boot-starter-security` (reactive), `spring-cloud-starter-netflix-eureka-client`, `spring-boot-starter-actuator`, `jjwt`.

- [ ] **Step 2: Create application class**
  `@SpringBootApplication` + `@EnableDiscoveryClient`. Port 8080.

- [ ] **Step 3: Create bootstrap.yml**
  Points to config-service.

- [ ] **Step 4: Create application.yml**
  Local dev config with route definitions.

- [ ] **Step 5: Commit**
  `feat(api-gateway): scaffold gateway project`

---

### Task 2: Route Configuration

**Files:**
- Modify: `api-gateway/src/main/resources/application.yml`
- Modify: `config-repo/api-gateway.yml`

- [ ] **Step 1: Define routes**
  Route definitions with service discovery:
  - `/api/users/**` → `lb://user-service`
  - `/api/companies/**` → `lb://company-service`
  - `/api/officers/**` → `lb://officer-service`
  Path rewriting as needed. Load-balanced via Eureka (`lb://`).

- [ ] **Step 2: Configure CORS**
  Allow all origins in dev. Configurable per profile.

- [ ] **Step 3: Add config-repo entry**
  `config-repo/api-gateway.yml` with docker profile overrides.

- [ ] **Step 4: Commit**
  `feat(api-gateway): add route configuration and CORS`

---

### Task 3: JWT Authentication Filter

**Files:**
- Create: `api-gateway/src/main/java/com/company/apigateway/security/JwtAuthenticationFilter.java`
- Create: `api-gateway/src/main/java/com/company/apigateway/security/JwtTokenValidator.java`
- Create: `api-gateway/src/main/java/com/company/apigateway/security/SecurityConfig.java`
- Create: `api-gateway/src/test/java/com/company/apigateway/security/` (tests)

- [ ] **Step 1: Create JwtTokenValidator**
  Validates JWT signature and expiration. Extracts claims. Uses same shared secret as user-service.

- [ ] **Step 2: Create JwtAuthenticationFilter**
  `GatewayFilter` or `GlobalFilter`. Extracts Bearer token, validates, passes request downstream if valid, returns 401 if invalid.

- [ ] **Step 3: Create SecurityConfig**
  Reactive security config. Public paths: `/api/users/signup`, `/api/users/signin`, `/api/users/auth/refresh`, `/actuator/health`. All others require valid JWT.

- [ ] **Step 4: Write integration tests**
  Test: request without token → 401, request with invalid token → 401, request with valid token → routed (or 503 if service not up), public endpoints pass without token.

- [ ] **Step 5: Commit**
  `feat(api-gateway): add JWT validation filter and security config`

---

### Task 4: Dockerization

**Files:**
- Create: `api-gateway/Dockerfile`
- Modify: `docker-compose.yml`

- [ ] **Step 1: Create Dockerfile**
  Multi-stage. Port 8080.

- [ ] **Step 2: Add to docker-compose.yml**
  Depends on config-service, registry-service. Health check on `/actuator/health`.

- [ ] **Step 3: Verify end-to-end**
  Full stack up. Signup through gateway (`localhost:8080/api/users/signup`), signin, use token on protected endpoint.

- [ ] **Step 4: Commit**
  `feat(api-gateway): add Docker support and compose integration`
