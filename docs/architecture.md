# Service Architecture

This diagram shows all inter-service relationships: HTTP routing through the gateway, synchronous Feign calls between business services, asynchronous Kafka event flows, and shared infrastructure dependencies.

```mermaid
graph TB
    Client(["Client"])

    subgraph Support["Support Services"]
        Config["config-service\n:8888"]
        Registry["registry-service\n:8761"]
    end

    subgraph Gateway["API Gateway"]
        GW["api-gateway\n:8080\nJWT validation · routing"]
    end

    subgraph Business["Business Services"]
        US["user-service\n:8081\nUsers · Auth · JWT"]
        CS["company-service\n:8082\nCompanies"]
        OS["officer-service\n:8083\nOfficers · Links"]
    end

    subgraph Messaging["Async Messaging"]
        UE[["user-events"]]
        CE[["company-events"]]
        OE[["officer-events"]]
    end

    subgraph DataStores["Data Stores"]
        PG[("PostgreSQL\nwrite store")]
        MG[("MongoDB\nread store")]
    end

    %% Client → Gateway → Services
    Client -->|"HTTP"| GW
    GW -->|"/api/users/**"| US
    GW -->|"/api/companies/**"| CS
    GW -->|"/api/officers/**"| OS

    %% Kafka producers
    US -->|"publish"| UE
    CS -->|"publish"| CE
    OS -->|"publish"| OE

    %% Kafka consumers (cross-service)
    CE -->|"consume"| OS
    OE -->|"consume"| CS

    %% Feign sync calls
    CS -. "GET /api/officers/by-company/{id}" .-> OS
    OS -. "GET /api/companies/{id}" .-> CS

    %% Data stores
    US --- PG & MG
    CS --- PG & MG
    OS --- PG & MG

    %% Support services (config + discovery)
    Config -.->|"config"| GW & US & CS & OS & Registry
    Registry -.->|"discovery"| GW & CS & OS

    %% Styles
    classDef service fill:#dbeafe,stroke:#2563eb,color:#1e3a8a
    classDef gateway fill:#e0e7ff,stroke:#4f46e5,color:#1e1b4b
    classDef support fill:#f3f4f6,stroke:#9ca3af,color:#374151
    classDef topic fill:#fef9c3,stroke:#ca8a04,color:#713f12
    classDef store fill:#dcfce7,stroke:#16a34a,color:#14532d

    class US,CS,OS service
    class GW gateway
    class Config,Registry support
    class UE,CE,OE topic
    class PG,MG store
```

## Legend

| Arrow style | Meaning |
|---|---|
| Solid `-->` | Synchronous HTTP (gateway routing) |
| Dotted `-.->`| Synchronous HTTP (Feign inter-service call) |
| Solid `-->` (Kafka) | Async event published to Kafka topic |
| Dashed `-.->`(support) | Config fetch / service discovery |

## Key relationships

**Gateway routing** — all external traffic enters through the api-gateway on port 8080. The gateway validates the JWT signature before forwarding to the target service.

**Feign calls (synchronous)** — two bidirectional sync dependencies exist between company-service and officer-service. Both are wrapped in Resilience4j circuit breakers with distinct fallback strategies: company-service degrades gracefully (returns the company without officers + a warning), while officer-service fails fast (503) to protect data integrity when linking an officer to an unverified company.

**Kafka events (asynchronous)** — each service publishes domain events to its own topic. Cross-service consumers: officer-service listens to `company-events` (e.g. company deleted → clean up links), company-service listens to `officer-events` (e.g. officer updated → refresh read model). Each service also consumes its own topic for internal CQRS read-model sync (PostgreSQL → MongoDB).

**Data stores** — every business service owns two stores: PostgreSQL (write/command side, source of truth) and MongoDB (read/query side, denormalized projections). No service accesses another service's database.
