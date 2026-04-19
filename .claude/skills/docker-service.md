# Docker & Docker Compose

## When to Use

When creating Dockerfiles for services, modifying Docker Compose configuration, or adding new infrastructure containers.

## Dockerfile Pattern (Multi-Stage)

Every service uses the same multi-stage pattern:

**Stage 1 — Build:**
- Base: `maven:3.9-eclipse-temurin-21` (or similar)
- Copy `pom.xml` first, download dependencies (layer caching)
- Copy source, run `mvn clean package -DskipTests`

**Stage 2 — Runtime:**
- Base: `eclipse-temurin:21-jre-alpine`
- Copy jar from build stage
- Expose service port
- `ENTRYPOINT ["java", "-jar", "app.jar"]`
- Configure JVM flags via `JAVA_OPTS` environment variable

## Docker Compose Files

### docker-compose.infra.yml

Infrastructure only — databases, messaging, config, registry:

| Container | Image | Port | Health Check |
|---|---|---|---|
| postgres | postgres:16 | 5432 | `pg_isready` |
| mongodb | mongo:7 | 27017 | `mongosh --eval "db.runCommand('ping')"` |
| zookeeper | confluentinc/cp-zookeeper | 2181 | — |
| kafka | confluentinc/cp-kafka | 9092 | kafka-topics command |
| config-service | custom build | 8888 | `/actuator/health` |
| registry-service | custom build | 8761 | `/actuator/health` |

PostgreSQL init script creates all databases: `user_db`, `company_db`, `officer_db`.
MongoDB databases are created on first write: `user_query_db`, `company_query_db`, `officer_query_db`.

### docker-compose.yml

Full stack — extends infra + business services:

| Container | Port | Depends on |
|---|---|---|
| api-gateway | 8080 | config-service, registry-service |
| user-service | 8081 | config-service, registry-service, postgres, mongodb, kafka |
| company-service | 8082 | config-service, registry-service, postgres, mongodb, kafka |
| officer-service | 8083 | config-service, registry-service, postgres, mongodb, kafka |

## Startup Order

Managed via `depends_on` with `condition: service_healthy`:
1. postgres, mongodb, zookeeper → kafka
2. config-service (must be ready before services fetch config)
3. registry-service
4. api-gateway + business services (parallel)

## Environment Variables (.env)

```
# Database
POSTGRES_USER=admin
POSTGRES_PASSWORD=<change-me>
MONGO_INITDB_ROOT_USERNAME=admin
MONGO_INITDB_ROOT_PASSWORD=<change-me>

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Config
CONFIG_SERVER_URI=http://config-service:8888
EUREKA_URI=http://registry-service:8761/eureka

# JWT
JWT_SECRET=<change-me>

# Default Admin
ADMIN_EMAIL=admin@company.com
ADMIN_PASSWORD=<change-me>
```

## Adding a New Service

1. Create `Dockerfile` in the service root following the multi-stage pattern
2. Add service to `docker-compose.yml` with correct dependencies and health check
3. Add database entries if needed (PostgreSQL init script + MongoDB auto-creates)
4. Add config file in `config-repo/`
5. Expose appropriate port

## Networking

All services share a single Docker network. Service names in Docker Compose match Spring application names so Eureka registration works transparently.