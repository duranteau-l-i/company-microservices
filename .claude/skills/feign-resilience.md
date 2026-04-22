# OpenFeign & Resilience4j

## When to Use

When implementing synchronous inter-service communication with fault tolerance.

## Feign Clients

### Where They Live

`infrastructure/feign/` in the calling service.

### Service Discovery

Feign clients resolve service names via Eureka — no hardcoded URLs:

```
@FeignClient(name = "officer-service", fallbackFactory = OfficerClientFallbackFactory.class)
```

The `name` matches the service's `spring.application.name` registered in Eureka.

### Configuration

Per client:
- Connect timeout: 3 seconds
- Read timeout: 5 seconds
- Retry: 2 attempts with exponential backoff

## Existing Feign Clients

| Caller | Target | Purpose |
|---|---|---|
| company-service | officer-service | Fetch officers for a company |
| officer-service | company-service | Validate companyId exists before linking |

## Resilience4j Circuit Breaker

### Configuration

- Sliding window: 10 calls
- Failure rate threshold: 50%
- Open state duration: 30 seconds before half-open
- Half-open: allow 3 calls to test recovery

### Fallback Strategies

Different strategies depending on the impact:

**Graceful degradation (company → officer):**
When officer-service is down, return the company without officers + a warning field:
- Response includes `officers: null` and `warnings: ["Officer service unavailable"]`
- The user still gets useful data — just incomplete
- Use a `FallbackFactory` to log the actual error

**Fail fast (officer → company):**
When company-service is down, reject the link operation:
- Return 503 with message: "Cannot verify company — try again later"
- Linking to a non-verified company risks data integrity
- Better to fail than to create broken links

## Key Rules

- **No synchronous chains**: a request never triggers A → B → C synchronously
- If a workflow needs multiple services, use Kafka events for the propagation
- Feign clients define their own local DTOs — no shared types with the target service
- Always define a fallback — never let a Feign call crash the caller

## Testing

- Unit tests: Feign clients are behind port interfaces, use InMemory adapters
- Integration tests: use WireMock to simulate the target service's responses (success, timeout, 500)
- E2E tests: real services running, test actual circuit breaker behavior