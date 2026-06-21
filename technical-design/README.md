# Technical Design

This folder is the architectural source of truth for the platform: an event-driven,
microservices e-commerce system built with Java 21 / Spring Boot 3.3, PostgreSQL,
Redis Streams, and gRPC.

It is written to be read on its own, before reading any code. If you only have ten
minutes, read [The system in one page](#the-system-in-one-page) below and the
[C4 Level 1 — System Context](c4/L1-system-context.md).

## How this is organised

The documentation follows three complementary lenses. Each answers a different
question and links to the others where they intersect.

| Folder | Lens | Answers |
|---|---|---|
| [`c4/`](c4/) | **Structure** — C4 model | *What are the pieces and how do they fit?* Context → Containers → Components → Code, zooming in one level at a time. |
| [`adr/`](adr/) | **Decisions** — Architecture Decision Records | *Why is it built this way, and what did we reject?* One record per significant, hard-to-reverse choice. |
| [`features/`](features/) | **Behaviour** — Feature specifications | *What does it actually do?* End-to-end flows, edge cases, and failure handling per capability. |

Read them in that order for a first pass — C4 for the shape, ADRs for the
rationale, features for the behaviour. They cross-reference heavily: a feature
spec points to the ADRs that constrain it; an ADR points to the C4 container it
shapes.

## The system in one page

A customer browses a product catalogue, places an order, and pays. That single
sentence touches every architectural decision in this repository.

- **Six services**, each owning its own data and one bounded context: **Gateway**
  (edge — routing, auth, rate limiting), **Users** (identity, JWT, RBAC),
  **Products** (catalogue, search, inventory, CSV import), **Orders** (order
  lifecycle, stock reservation), **Payments** (a deliberately fake processor),
  and **Notifications** (a pure event consumer that emails customers).
- **Three communication styles, each chosen for a reason.** REST at the edge for
  the browser. **gRPC** for the one synchronous internal hop on the hot path
  (Orders checking stock with Products). **Redis Streams** for everything async —
  the event backbone that lets services react without calling each other.
- **The purchase flow is a Saga.** When an order is placed, no single coordinator
  drives it to completion. Instead services *choreograph*: `order.placed` triggers
  Payments; `payment.succeeded`/`payment.failed` drives Orders to `PAID` or
  `FAILED`; a failure path *compensates* by releasing reserved stock and notifying
  the customer. There is no distributed transaction — consistency is eventual and
  achieved through events plus compensation.
- **One PostgreSQL instance, five schemas, no shared tables.** Each service reads
  and writes only its own schema. This is service-data-ownership without the
  operational cost of five databases — and the documented path to true
  database-per-service is a config change, not a rewrite.
- **Redis does four distinct jobs**: the event bus (Streams), a short-TTL read
  cache, rate-limit counters, and the idempotency store. It is never the source
  of truth, and it never caches stock levels.

The headline trade-off — that this is intentionally *more* system than a take-home
strictly requires — is stated plainly in the repository [scope note](#scope-note).

## Document index

### C4 model — [`c4/`](c4/)
| Level | Document | Scope |
|---|---|---|
| L1 | [System Context](c4/L1-system-context.md) | The platform as one box; its users and external systems. |
| L2 | [Containers](c4/L2-containers.md) | The six services + datastores; protocols between them. |
| L3 | [Components](c4/L3-components.md) | Internal building blocks of each service. |
| L4 | [Code](c4/L4-code.md) | The hexagonal layering every service shares. |

### Architecture Decision Records — [`adr/`](adr/)
Sixteen records covering the load-bearing choices — datastore, event bus, saga
style, hexagonal architecture, functional Java, gateway, observability, test
strategy, gRPC, service discovery, CQRS, OLTP/OLAP separation, repository layout,
API documentation, the concurrency/locking model, and the demo-seeding strategy.
See the [ADR index](adr/README.md) for the full list and status table.

### Feature specifications — [`features/`](features/)
Behavioural specs for each core capability — authentication & RBAC, catalogue &
search, CSV import, order placement & stock reservation, the purchase saga, and
notifications. See the [feature index](features/README.md).

## Conventions

- **Diagrams are [Mermaid](https://mermaid.js.org/)**, embedded in the Markdown.
  They render natively on GitHub and version like text — no binary image assets
  to drift out of sync with the prose. Rendered PNG exports, where needed for the
  formal design doc, live separately under `docs/design/assets/`.
- **ADR status** is one of `Proposed`, `Accepted`, `Superseded`, or `Deprecated`.
- **Implementation tier** is called out wherever it matters, because not every
  documented capability is built. Three tiers:
  - **Implemented** — built and tested in this repository.
  - **Illustrative** — one worked example plus documentation of the pattern
    (e.g. a single Spring Cloud Contract, one dead-letter-queue example).
  - **Production path** — deliberately *not* built; documented as the next step
    with its rationale (e.g. Kafka, Kubernetes, Temporal, the Clojure migration).

## Scope note

> The implementation scope of this project deliberately extends beyond the stated
> challenge requirements. This is intentional.
>
> The extended scope reflects my current level of experience and the kind of
> systems I design and operate professionally. It is not an attempt to
> over-engineer a simple problem without awareness of the trade-offs involved.
>
> A simpler solution (monolith, single database, no message queue) would fully
> satisfy the requirements and would be the correct choice in many real-world
> contexts. That trade-off is explicitly documented in the design doc, including
> when each architectural decision adds value and when it would not.
