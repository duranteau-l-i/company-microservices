# Phase 1: Infrastructure

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set up Docker Compose infrastructure, Config Service, and Registry Service so that business services have a runtime environment to register into and fetch configuration from.

**Architecture:** Docker Compose orchestrates all infrastructure (PostgreSQL, MongoDB, Kafka, Zookeeper). Config Service serves centralized YAML config. Registry Service provides service discovery.

**Tech Stack:** Docker, Docker Compose, Spring Cloud Config Server, Spring Cloud Netflix Eureka

---

### Task 1: Docker Compose Infrastructure

**Files:**
- Create: `docker-compose.infra.yml`
- Create: `.env`
- Create: `scripts/init-postgres.sql`
- Create: `.gitignore`

- [ ] **Step 1: Create `.gitignore`**
  Standard Java/Maven/IDE/Docker ignores. Include `.env` in gitignore for production but track a `.env.example`.

- [ ] **Step 2: Create `.env.example`**
  Template with all required environment variables (no real secrets).

- [ ] **Step 3: Create `.env`**
  Local development values (these won't be committed).

- [ ] **Step 4: Create PostgreSQL init script**
  `scripts/init-postgres.sql` — creates `user_db`, `company_db`, `officer_db` databases.

- [ ] **Step 5: Create `docker-compose.infra.yml`**
  Services: postgres (with init script mount), mongodb, zookeeper, kafka.
  All with health checks. Single Docker network.

- [ ] **Step 6: Verify infrastructure starts**
  Run: `docker compose -f docker-compose.infra.yml up -d`
  Verify: all containers healthy, databases created, Kafka broker accessible.

- [ ] **Step 7: Commit**
  `chore: add docker compose infrastructure with PostgreSQL, MongoDB, and Kafka`

---

### Task 2: Config Service

**Files:**
- Create: `config-service/pom.xml`
- Create: `config-service/src/main/java/com/company/configservice/ConfigServiceApplication.java`
- Create: `config-service/src/main/resources/application.yml`
- Create: `config-service/Dockerfile`

- [ ] **Step 1: Create Maven project**
  `pom.xml` with parent `spring-boot-starter-parent` (3.x). Dependencies: `spring-cloud-config-server`, `spring-boot-starter-actuator`.

- [ ] **Step 2: Create application class**
  `@SpringBootApplication` + `@EnableConfigServer`. Port 8888.

- [ ] **Step 3: Create `application.yml`**
  Configure to serve from local `config-repo/` directory. Actuator health endpoint enabled.

- [ ] **Step 4: Create Dockerfile**
  Multi-stage build: Maven build → JRE runtime.

- [ ] **Step 5: Add to `docker-compose.infra.yml`**
  Add config-service with health check on `/actuator/health`. Depends on nothing.

- [ ] **Step 6: Verify Config Service starts**
  Build and run. Verify: `/actuator/health` returns UP.

- [ ] **Step 7: Commit**
  `feat(config-service): add Spring Cloud Config Server`

---

### Task 3: Config Repository

**Files:**
- Create: `config-repo/application.yml`
- Create: `config-repo/user-service.yml`
- Create: `config-repo/company-service.yml`
- Create: `config-repo/officer-service.yml`
- Create: `config-repo/api-gateway.yml`

- [ ] **Step 1: Create shared `application.yml`**
  Common config: Eureka client settings, actuator exposure, JWT secret placeholder, Kafka bootstrap servers.

- [ ] **Step 2: Create service-specific configs**
  Each file contains: server port, database connection (PostgreSQL + MongoDB), service-specific settings. Use profile-based overrides for `docker` profile.

- [ ] **Step 3: Verify Config Service serves configs**
  Start config-service, hit `http://localhost:8888/user-service/default`. Should return the config.

- [ ] **Step 4: Commit**
  `chore(config-repo): add centralized configuration for all services`

---

### Task 4: Registry Service

**Files:**
- Create: `registry-service/pom.xml`
- Create: `registry-service/src/main/java/com/company/registryservice/RegistryServiceApplication.java`
- Create: `registry-service/src/main/resources/application.yml`
- Create: `registry-service/Dockerfile`

- [ ] **Step 1: Create Maven project**
  `pom.xml` with `spring-cloud-starter-netflix-eureka-server`, `spring-boot-starter-actuator`.

- [ ] **Step 2: Create application class**
  `@SpringBootApplication` + `@EnableEurekaServer`. Port 8761.

- [ ] **Step 3: Create `application.yml`**
  Self-registration disabled (`registerWithEureka: false`, `fetchRegistry: false`). Actuator health enabled.

- [ ] **Step 4: Create Dockerfile**
  Same multi-stage pattern as config-service.

- [ ] **Step 5: Add to `docker-compose.infra.yml`**
  Add registry-service with health check. Depends on config-service (fetches config from it via bootstrap.yml) — or standalone config for simplicity.

- [ ] **Step 6: Verify Registry Service starts**
  Build and run. Verify: Eureka dashboard at `http://localhost:8761`, `/actuator/health` returns UP.

- [ ] **Step 7: Commit**
  `feat(registry-service): add Eureka service registry`

---

### Task 5: Full Infrastructure Validation

- [ ] **Step 1: Create `docker-compose.yml`**
  Extends `docker-compose.infra.yml`, adds placeholders for business services (empty for now). Ensures the full compose file structure works.

- [ ] **Step 2: Start full infrastructure**
  Run: `docker compose -f docker-compose.infra.yml up -d`
  Verify: all containers healthy, config-service serves config, registry-service dashboard shows no registered services.

- [ ] **Step 3: Commit**
  `chore: add full docker-compose.yml and validate infrastructure stack`