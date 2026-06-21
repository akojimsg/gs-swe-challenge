# Architecture Decision Records

An ADR captures a single significant architectural decision: the context that
forced a choice, the decision taken, the alternatives weighed, and the
consequences accepted. They are immutable once accepted — a decision that changes
gets a *new* ADR that supersedes the old one, rather than an edit. This gives a
durable record of *why* the system is the way it is, which is worth far more to a
future engineer than a description of *what* it is.

These records use a lightweight [MADR](https://adr.github.io/madr/)-style format:
**Context → Decision → Alternatives Considered → Consequences → Status**.

## Index

| ADR | Decision | Shapes | Status |
|---|---|---|---|
| [001](ADR-001-database.md) | PostgreSQL over MongoDB; schema-per-service isolation | Data layer | Accepted |
| [002](ADR-002-event-bus.md) | Redis Streams over Kafka for the event bus | Async backbone | Accepted |
| [003](ADR-003-choreography.md) | Saga **choreography** over orchestration | Purchase flow | Accepted |
| [004](ADR-004-hexagonal-arch.md) | Hexagonal (ports & adapters) per service | Every service | Accepted |
| [005](ADR-005-functional-java.md) | Functional Java style (immutable, no setters) | Domain code | Accepted |
| [006](ADR-006-api-gateway.md) | Spring Cloud Gateway as single entry point | Edge | Accepted |
| [007](ADR-007-observability.md) | Grafana stack (Prometheus/Loki/Jaeger) over Datadog | Observability | Accepted |
| [008](ADR-008-testcontainers.md) | Testcontainers over H2 for integration tests | Test strategy | Accepted |
| [009](ADR-009-grpc-internal.md) | gRPC for the Orders→Products internal call | Internal comms | Accepted |
| [010](ADR-010-service-registry.md) | No service registry (Docker DNS) | Service discovery | Accepted |
| [011](ADR-011-cqrs.md) | CQRS at the application layer (Redis read model) | Products reads | Accepted |
| [012](ADR-012-oltp-olap.md) | OLTP/OLAP separation strategy | Analytics path | Accepted |
| [013](ADR-013-repo-layout.md) | Single root Gradle build in a polyglot monorepo | Repo layout | Accepted |
| [014](ADR-014-api-documentation.md) | Code-first OpenAPI via springdoc, per-service export | API docs | Accepted |
| [015](ADR-015-concurrency-and-locking.md) | Concurrency, locking (pessimistic vs optimistic), and latency model | Concurrency | Accepted |
| [016](ADR-016-frontend-stack.md) | React + Vite SPA with shadcn/ui, Zustand, Tailwind | Frontend | Accepted |

## Reading order

If reading cold, the decisions that explain the *most* about the system are, in
order: **003** (the saga — the system's defining behaviour), **004** (hexagonal —
how every service is shaped), **002** (the event bus — how services talk), and
**001** (data ownership). The rest refine the edges.

## A note on tiers

Several ADRs explicitly choose a *simpler-than-production* option and document the
production path as a non-goal-for-now (Kafka behind Streams, orchestration behind
choreography, K8s behind Docker Compose). This is the deliberate trade-off stated
in the repository [scope note](../README.md#scope-note): the architecture is shaped
so that each of these is a documented, low-friction upgrade rather than a rewrite.
