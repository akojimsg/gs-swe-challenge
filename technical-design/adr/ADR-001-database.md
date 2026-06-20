# ADR-001 — PostgreSQL over MongoDB, with schema-per-service isolation

**Status:** Accepted

## Context

The platform's core entities — orders, order items, payments — are inherently
relational and have hard integrity requirements. An order line references a
product; a payment references exactly one order; money fields must not lose
precision. The domain is also one a fintech end-client operates in, where
correctness of financial records is non-negotiable.

Separately, we need each service to own its data privately (a microservices
tenet) without paying the operational cost of running and backing up five
separate database instances for a take-home-scale system.

Two questions, therefore: **what kind of database**, and **how to partition it**.

## Decision

Use **PostgreSQL 16** as the single system of record, partitioned into **five
schemas** — one per service (`users_schema`, `products_schema`, `orders_schema`,
`payments_schema`, `notifications_schema`).

Each service connects only to its own schema. There are **no cross-schema queries
and no foreign keys spanning schemas.** A service that needs another's data calls
its API (sync) or consumes its events (async) — never reaches into its tables.

Each service owns and applies its own **Flyway** migrations.

We additionally lean on Postgres-native capabilities the design needs anyway:
- **Full-text search** via a GIN index (`to_tsvector`) — no separate search engine.
- `NUMERIC(10,2)` for money — exact decimal, never floating point.
- `CHECK` constraints (`price >= 0`, `stock >= 0`, `quantity > 0`) and row locking
  (`SELECT … FOR UPDATE`) that the stock-reservation flow depends on.

## Alternatives Considered

| Option | Why not |
|---|---|
| **MongoDB** | No multi-document ACID guarantees we'd want for order→payment consistency at this scale; relational integrity would have to be enforced in application code. Strength (flexible documents) is irrelevant to a fixed, well-known schema. |
| **One database per service (5 instances)** | The "correct" production end-state, but operationally heavy for this scope — 5× the backup, connection, and migration surface. Schema isolation gives 90% of the benefit (private, independently-migratable storage) at a fraction of the cost. |
| **A separate search engine (Elasticsearch/OpenSearch)** | Unjustified for a catalogue of this size; Postgres FTS handles the query patterns. Documented as a scale-up path. |

## Consequences

**Positive**
- Relational integrity and exact-decimal money where they matter.
- Services have private, independently-evolvable storage today.
- The path to database-per-service is a **connection-string change**, not a
  rewrite — the no-cross-schema-access discipline is what guarantees this.
- One instance to run, back up, and reason about locally.

**Negative / accepted**
- A shared instance is a shared blast radius: one Postgres outage takes down all
  services. Acceptable at this scale; the per-service-DB path mitigates it later.
- The schema-isolation discipline is enforced by convention and review, not by the
  database — a careless cross-schema join would compile. Caught in code review.

**Production path:** database-per-service; read replicas; PgBouncer connection
pooling; managed Postgres (RDS/Aurora). See also [ADR-012](ADR-012-oltp-olap.md)
for the analytics offload.

## Related

- [C4 L2 — Containers](../c4/L2-containers.md) · [ADR-011](ADR-011-cqrs.md) (read
  model) · [ADR-012](ADR-012-oltp-olap.md) (OLTP/OLAP)
