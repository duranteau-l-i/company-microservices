# Company Microservices — Master Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement each phase plan.

**Goal:** Build a microservices platform for managing companies and their officers with DDD, CQRS, and event-driven architecture.

**Architecture:** Hexagonal/DDD with CQRS (PostgreSQL write, MongoDB read, Kafka sync). Six independent Spring Boot services in a monorepo. JWT auth, fault-tolerant inter-service communication.

**Tech Stack:** Java 21, Spring Boot 3.x, Maven, Spring Cloud (Config, Eureka, Gateway), Kafka, PostgreSQL 16, MongoDB 7, Docker

---

## Phase Overview

Each phase produces a working, testable increment. Phases must be implemented in order — each builds on the previous.

| Phase | Plan File | What It Delivers |
|---|---|---|
| 1 | `01-infrastructure.md` | Docker Compose, config-service, registry-service, config-repo |
| 2 | `02-user-service.md` | User domain, auth (signup/signin/JWT), role management |
| 3 | `03-api-gateway.md` | Gateway with JWT validation, routing to services |
| 4 | `04-company-service.md` | Company CRUD, CQRS, authorization, Kafka events |
| 5 | `05-officer-service.md` | Officer CRUD, search, deduplication, company linking |
| 6 | `06-integration.md` | Cross-service Feign calls, fault tolerance, Kafka event sync, E2E tests |

## Dependencies

```
Phase 1 (Infrastructure)
  └── Phase 2 (User Service)
        └── Phase 3 (API Gateway)
              ├── Phase 4 (Company Service)
              └── Phase 5 (Officer Service)
                    └── Phase 6 (Integration)
```

## What "Done" Means per Phase

- All unit tests pass (InMemory pattern)
- All integration tests pass (Testcontainers)
- Service starts and registers with Eureka
- Docker Compose includes the service
- Config is in config-repo
- `README.md` exists at the service root covering architecture, endpoints, configuration, and testing instructions
- OpenAPI spec is exposed via springdoc-openapi; Swagger UI reachable at `/swagger-ui.html`
- Committed with conventional commit messages
