# ADR-006 — Spring Cloud Gateway as the single entry point

**Status:** Accepted

## Context

Five services expose HTTP APIs. Without a single front door, every service would
have to independently validate JWTs, enforce rate limits, set CORS, and the browser
would need to know five hosts. Cross-cutting edge concerns would be duplicated five
times and drift apart.

We want one place that: validates auth once, routes to the right service, protects
downstreams from overload and from each other's failures, and stamps every request
with a correlation ID for tracing.

## Decision

Put **Spring Cloud Gateway** in front of all services as the single entry point on
`:8080`. It owns **edge concerns only — no business logic**:

- **Routing** — maps `/api/**` paths to the correct service; declares which routes
  are **public** (no JWT): auth endpoints and product/category reads.
- **JWT validation** — verifies the access token's signature and expiry once, at
  the edge; downstream services trust the forwarded identity.
- **Rate limiting** — **Bucket4j** with buckets in Redis, keyed per route (by IP
  for public/auth routes, by userId for authenticated ones), so limits hold across
  Gateway replicas. Limits: 100/min public products, 10/min auth, 20/min orders,
  5/hour import, 60/min default.
- **Circuit breaking** — **Resilience4j** breaker per downstream; fails fast when a
  service is unhealthy instead of piling up blocked requests.
- **Request-ID injection** — generates/propagates the `traceId` into MDC and
  downstream headers ([ADR-007](ADR-007-observability.md)).

## Alternatives Considered

| Option | Why not |
|---|---|
| **No gateway (services exposed directly)** | Duplicates auth/rate-limit/CORS across five services and leaks topology to the browser. |
| **Nginx / Envoy / Kong** | Capable edge proxies, but JWT logic and Bucket4j integration would live outside the Java/Spring ecosystem, splitting the stack and the team's mental model. Spring Cloud Gateway keeps the edge in the same language and dependency set. |
| **Per-service filters via a shared library** | Keeps it in Java but still runs the edge logic N times and couples every service to the auth library version. A single gateway centralises it. |

## Consequences

**Positive**
- Auth, rate limiting, and tracing are defined **once**, consistently.
- Downstream services are simpler — they trust the edge and focus on their domain.
- Circuit breakers contain a failing service instead of letting it cascade.

**Negative / accepted**
- The gateway is a **single point of failure** and a potential bottleneck. Mitigated
  by it being stateless (Redis holds the rate-limit state) and therefore
  horizontally scalable; run multiple replicas in production.
- One more hop of latency. Negligible relative to the protection it provides.

**Production path:** multiple gateway replicas behind a load balancer; mTLS to
downstreams; WAF in front.

## Related

- [C4 L2 — Containers](../c4/L2-containers.md) ·
  [C4 L3 — Gateway](../c4/L3-components.md#gateway-8080) ·
  [Auth & RBAC](../features/auth-and-rbac.md)
