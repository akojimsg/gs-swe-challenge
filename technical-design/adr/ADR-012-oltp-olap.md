# ADR-012 — OLTP/OLAP separation strategy

**Status:** Accepted

## Context

Two workloads with opposing access patterns will eventually contend: **OLTP** (the
transactional path — placing orders, processing payments, lots of small
low-latency reads/writes) and **OLAP** (analytics — "revenue by category last
quarter," large scans and aggregations). Running heavy analytical queries against
the transactional database degrades the very latency the storefront depends on.

For a fintech client, the analytics/reporting need is a near-certainty, so the
design should not paint itself into a corner — even though building a full
analytics pipeline is out of scope for this submission.

## Decision

Keep the workloads separated **in strategy**, implemented minimally now and with a
clear production path:

- **OLTP** — PostgreSQL serves all transactional traffic ([ADR-001](ADR-001-database.md)).
  No analytical queries run against it.
- **Analytics seed** — the **event stream is the analytics source**. Every domain
  event already carries full state ([ADR-002](ADR-002-event-bus.md)), so the event
  log is a natural, append-only feed for downstream analytics — no need to mine the
  OLTP tables.
- **No OLAP store is built** in this submission (documented-only tier). The design
  simply ensures the seed (rich events) exists.

## Alternatives Considered

| Option | Why not (now) |
|---|---|
| **Run analytics directly on the OLTP Postgres** | Simplest, and fine until the first heavy report — at which point it contends with the storefront for resources and locks. The seed-via-events approach avoids designing this dead-end in. |
| **Build the OLAP pipeline now (Debezium → Kafka → warehouse)** | The correct production architecture, but a large build (CDC, streaming, a warehouse) far beyond a take-home's scope and unnecessary without real analytical demand. Documented as the path. |
| **A read replica for analytics** | A reasonable cheap first step (offload reads without a warehouse) — noted as an intermediate option, but still not built here. |

## Consequences

**Positive**
- Transactional latency is protected — no analytical load on the OLTP path.
- The rich event log already provides an analytics seed at zero extra cost.
- The production path is unobstructed: nothing in the current design has to be
  undone to add a warehouse.

**Negative / accepted**
- No analytics capability ships in this submission — it is strategy + seed only.
  Explicitly a non-goal; the value here is *not foreclosing* the option.

**Production path:** **Debezium** CDC (or the event stream) → Kafka → a columnar
warehouse (Redshift/Snowflake/BigQuery); optionally a read replica as an
intermediate step.

## Related

- [ADR-001](ADR-001-database.md) (OLTP store) · [ADR-002](ADR-002-event-bus.md)
  (events as the seed)
