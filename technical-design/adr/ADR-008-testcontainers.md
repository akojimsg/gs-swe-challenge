# ADR-008 — Testcontainers over H2 for integration tests

**Status:** Accepted

## Context

The system's correctness hinges on behaviour that only the *real* PostgreSQL and
Redis exhibit:

- **Stock reservation** uses `SELECT … FOR UPDATE` row locking under concurrency
  ([Order Placement](../features/order-placement.md)). An in-memory DB's locking
  semantics differ and would let a broken implementation pass.
- **Full-text search** uses a Postgres GIN index and `to_tsvector` — H2 has no
  equivalent.
- **Idempotency and the event bus** depend on Redis Streams semantics.

Tests that pass against a fake but fail against production are worse than no tests —
they manufacture false confidence in exactly the concurrency-sensitive code that
matters most.

## Decision

Use **Testcontainers** to run **real PostgreSQL 16 and Redis 7** in Docker for
integration tests (`@SpringBootTest` + `*IT.java`). The pure domain is still
unit-tested without any container ([ADR-004](ADR-004-hexagonal-arch.md)); the test
pyramid is:

- **Unit** — domain/use-case logic, plain JUnit + Mockito, milliseconds, no Docker.
- **Integration** — real PG + Redis via Testcontainers; the concurrency, locking,
  FTS, and event-flow tests live here.
- **Contract / E2E / perf / smoke** — see the [test-strategy spec](../features/README.md).

The integration suite explicitly includes a **concurrent stock-reservation test**
that would be meaningless against H2.

## Alternatives Considered

| Option | Why not |
|---|---|
| **H2 (in-memory) in PostgreSQL-compat mode** | Fast and zero-setup, but its locking, FTS, and type behaviour diverge from real Postgres — precisely where our risk is. False greens on concurrency tests. |
| **Embedded Redis (e.g. a Java fake)** | Approximations of Streams/consumer-group semantics; same false-confidence problem. |
| **A shared, long-lived test database** | Stateful, order-dependent, flaky across parallel runs and CI. Testcontainers gives a clean, isolated instance per run. |

## Consequences

**Positive**
- Integration tests exercise **production-equivalent** datastore behaviour —
  locking, FTS, and Streams are the real thing.
- Each run is isolated and reproducible locally and in CI; no shared mutable test DB.

**Negative / accepted**
- Slower than in-memory (container start-up per suite) and **requires a Docker
  daemon** in CI. Mitigated by container reuse and running integration tests as a
  distinct CI stage from fast unit tests.
- Adds the Testcontainers dependency and Docker as a test prerequisite. Acceptable
  given the correctness stakes.

## Related

- [ADR-001](ADR-001-database.md) (the locking/FTS behaviour under test) ·
  [Order Placement](../features/order-placement.md) (the concurrency test target)
