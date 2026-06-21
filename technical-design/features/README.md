# Feature Specifications

Where the [C4 model](../c4/) describes structure and the [ADRs](../adr/) capture
decisions, these specs describe **behaviour**: what each capability does, the
end-to-end flow, the edge cases it must handle, and how it fails. They are written
to be implementable — close enough to the metal to build from, without prescribing
class-by-class internals (that is the code's job).

## Index

| Feature | Service(s) | What it covers |
|---|---|---|
| [Authentication & RBAC](auth-and-rbac.md) | Users, Gateway | Registration, login, JWT access + refresh tokens, role-based authorization. |
| [Catalogue & Search](catalogue-and-search.md) | Products | Browsing, filtering, full-text search, categories, the read cache. |
| [CSV Import](csv-import.md) | Products | Async bulk import: validation, sanitisation, upsert-by-SKU, the messy-data edge cases. |
| [Order Placement & Stock Reservation](order-placement.md) | Orders, Products | Idempotent placement, the gRPC stock check, `SELECT … FOR UPDATE` reservation, concurrency. |
| [Purchase Saga](purchase-saga.md) | Orders, Payments, Products, Notifications | The choreographed payment flow — happy path and compensation. |
| [Notifications](notifications.md) | Notifications | The pure event consumer, templates, idempotent delivery, the log. |
| [Frontend SPA](frontend-spa.md) | Frontend | The React SPA: routes→API, the purchase + CSV-import journeys, state/auth handling, how it consumes the Gateway contract. |

## Cross-cutting behaviours

Some behaviours recur across every feature; they are stated once here rather than
repeated in each spec.

- **Idempotency** — anywhere an action could be retried or an event redelivered, it
  must be safe to apply twice. Orders dedupes on the `Idempotency-Key` header;
  every event consumer dedupes on `eventId`. Detailed where it bites:
  [Order Placement](order-placement.md), [Purchase Saga](purchase-saga.md),
  [Notifications](notifications.md).
- **Authorization** — every non-public endpoint is gated by role (`BUYER`/`ADMIN`)
  at the Gateway and re-checked in the service. The public surface is exactly: auth
  endpoints and product/category reads. See [Auth & RBAC](auth-and-rbac.md).
- **Validation at boundaries** — all external input (request bodies, CSV rows,
  event payloads) is validated and sanitised at the system edge before it reaches
  the domain.
- **Observability** — every flow emits a counter + latency histogram and structured
  logs correlated by `traceId` ([ADR-007](../adr/ADR-007-observability.md)).

## The event catalogue

Most cross-service behaviour is carried by 15 events over Redis Streams
([ADR-002](../adr/ADR-002-event-bus.md)). The catalogue — publishers and
subscribers — is documented in the [Purchase Saga](purchase-saga.md#event-catalogue)
spec, since that is where the event choreography is densest.

## Test strategy (summary)

Each feature is covered by the test pyramid ([ADR-008](../adr/ADR-008-testcontainers.md)):

| Layer | Tooling | Targets |
|---|---|---|
| Unit | JUnit 5 + Mockito + AssertJ | Domain rules, use cases (e.g. `CsvValidator`, order state machine). |
| Integration | Spring Boot Test + **Testcontainers** (real PG + Redis) | Auth flow, purchase flow, idempotency, **concurrent stock reservation**. |
| Contract | Spring Cloud Contract | One illustrative Orders `POST` contract. |
| E2E | Playwright | Purchase flow, CSV import with errors, admin CRUD. |
| Performance | k6 | Product search, purchase flow. |
| Smoke | Shell | Post-deploy health of every service. |
