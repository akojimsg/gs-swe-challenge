# C4 Level 2 — Containers

Stepping inside the [platform box](L1-system-context.md). A "container" in C4 is a
separately runnable/deployable thing — a service process, a database, a cache — not
a Docker container specifically (though here they mostly map one-to-one).

## Diagram

```mermaid
graph TB
    browser["<b>React SPA</b><br/><i>Container: JS / Vite</i><br/>Catalogue, cart, checkout,<br/>admin console"]

    subgraph platform["E-Commerce Platform"]
        gateway["<b>Gateway</b> :8080<br/><i>Spring Cloud Gateway</i><br/>Routing, JWT validation,<br/>rate limiting, circuit breakers"]

        users["<b>Users</b> :8081<br/><i>Spring Boot</i><br/>Identity, JWT issuance,<br/>RBAC"]
        products["<b>Products</b> :8082<br/><i>Spring Boot + gRPC server</i><br/>Catalogue, search, inventory,<br/>CSV import"]
        orders["<b>Orders</b> :8083<br/><i>Spring Boot + gRPC client</i><br/>Order lifecycle,<br/>stock reservation"]
        payments["<b>Payments</b> :8084<br/><i>Spring Boot</i><br/>Fake payment processor"]
        notifications["<b>Notifications</b> :8085<br/><i>Spring Boot</i><br/>Pure event consumer,<br/>email + delivery log"]

        postgres[("<b>PostgreSQL 16</b><br/><i>one instance, 5 schemas</i><br/>System of record")]
        redis[("<b>Redis 7</b><br/><i>Streams · cache ·<br/>rate-limit · idempotency</i>")]
    end

    mail["<b>Email / SMTP</b><br/><i>Mailhog locally</i>"]

    browser -->|"JSON / REST<br/>HTTPS"| gateway

    gateway -->|"REST"| users
    gateway -->|"REST"| products
    gateway -->|"REST"| orders
    gateway -->|"REST"| payments
    gateway -->|"REST"| notifications

    orders -->|"<b>gRPC</b><br/>stock check<br/>(sync, hot path)"| products

    users -.->|"publish/consume<br/><b>events</b>"| redis
    products -.->|"events"| redis
    orders -.->|"events"| redis
    payments -.->|"events"| redis
    notifications -.->|"consume events"| redis

    users --> postgres
    products --> postgres
    orders --> postgres
    payments --> postgres
    notifications --> postgres

    products -->|"cache<br/>read model"| redis
    gateway -->|"rate-limit<br/>counters"| redis
    orders -->|"idempotency<br/>keys"| redis

    notifications -->|"SMTP"| mail

    classDef svc fill:#1168bd,stroke:#0b4884,color:#fff
    classDef store fill:#2e7d32,stroke:#1b5e20,color:#fff
    classDef ext fill:#999,stroke:#6b6b6b,color:#fff
    class gateway,users,products,orders,payments,notifications svc
    class postgres,redis store
    class browser,mail ext
```

> **Edge legend.** Solid arrows are **synchronous** (REST/gRPC, request blocks on
> response). Dashed arrows are **asynchronous** events over Redis Streams (fire and
> forget; consumers react independently). This distinction is the single most
> important thing to read off this diagram.

## The six services

| Service | Port | Bounded context | Talks to | Key feature spec |
|---|---|---|---|---|
| **Gateway** | 8080 | Edge concerns only — *no business logic*. Validates JWTs once, routes to services, enforces rate limits, trips circuit breakers per downstream, injects a request/trace ID. | All services (REST); Redis (rate-limit counters). | [ADR-006](../adr/ADR-006-api-gateway.md) |
| **Users** | 8081 | Identity: registration, login, JWT issuance, refresh-token rotation, roles (`BUYER`/`ADMIN`), profiles. | PostgreSQL (`users_schema`); Redis (events). | [Auth & RBAC](../features/auth-and-rbac.md) |
| **Products** | 8082 | Catalogue, categories, full-text search, inventory, async CSV import. **Hosts the gRPC stock-check server.** | PostgreSQL (`products_schema`); Redis (cache + events). | [Catalogue & Search](../features/catalogue-and-search.md), [CSV Import](../features/csv-import.md) |
| **Orders** | 8083 | Order lifecycle, stock reservation (`SELECT … FOR UPDATE`), idempotent placement, saga participant. **gRPC client to Products.** | PostgreSQL (`orders_schema`); Products (gRPC); Redis (idempotency + events). | [Order Placement](../features/order-placement.md), [Purchase Saga](../features/purchase-saga.md) |
| **Payments** | 8084 | Fake payment processing (90% success / 10% failure), payment records, saga participant. | PostgreSQL (`payments_schema`); Redis (events). | [Purchase Saga](../features/purchase-saga.md) |
| **Notifications** | 8085 | **Pure event consumer.** Renders email templates, sends via SMTP, records a delivery log. Exposes only one admin read endpoint. | PostgreSQL (`notifications_schema`); Redis (consume); SMTP. | [Notifications](../features/notifications.md) |

## The two datastores

### PostgreSQL 16 — system of record
One instance, **five schemas** (`users_schema`, `products_schema`,
`orders_schema`, `payments_schema`, `notifications_schema`). Each service owns its
schema exclusively — **no cross-schema queries, no foreign keys across schema
boundaries.** This buys service-data-ownership (a service's storage is private and
independently evolvable) while paying single-instance operational cost. The path
to a true database-per-service is then a connection-string change, documented in
[ADR-001](../adr/ADR-001-database.md). Each service owns its schema migrations via
**Flyway**.

### Redis 7 — four distinct roles
Redis is load-bearing but is **never the source of truth**:

1. **Event bus** — Redis Streams carry all 15 async events between services
   ([ADR-002](../adr/ADR-002-event-bus.md)).
2. **Read cache** — Products caches list/search results, TTL 60s. **Stock levels
   are never cached** — they must be read live to keep reservation correct.
3. **Rate-limit counters** — Bucket4j stores its buckets in Redis so limits hold
   across Gateway replicas ([ADR-006](../adr/ADR-006-api-gateway.md)).
4. **Idempotency store** — Orders persists idempotency keys (TTL 24h) so a retried
   submission returns the original result instead of creating a duplicate.

## Communication styles — and why each

| Style | Where | Why here |
|---|---|---|
| **REST/JSON** | Browser → Gateway → services. | Universal, cacheable, the natural fit for a browser client and external surface. |
| **gRPC** | Orders → Products (stock check). | The one synchronous internal call on the order hot path. Binary, strongly-typed contract, low latency. [ADR-009](../adr/ADR-009-grpc-internal.md). |
| **Redis Streams (events)** | Everything async — the saga, notifications, stock-depletion alerts, etc. | Decouples producers from consumers, absorbs load, and enables choreography without services synchronously calling each other. [ADR-002](../adr/ADR-002-event-bus.md). |

The discipline: **synchronous only where a caller genuinely needs an answer to
proceed** (Orders cannot reserve stock it hasn't confirmed exists). Everything that
can be a reaction is an event.

## Cross-cutting at this level

- **Service discovery**: none. Docker DNS resolves service names on a single host.
  Consul/K8s DNS is the documented scale-up ([ADR-010](../adr/ADR-010-service-registry.md)).
- **Observability**: every container exposes `/actuator/health` and
  `/actuator/prometheus`; emits structured JSON logs and OpenTelemetry traces, all
  correlated by `traceId` ([ADR-007](../adr/ADR-007-observability.md)).
