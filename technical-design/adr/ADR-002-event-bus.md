# ADR-002 — Redis Streams over Kafka for the event bus

**Status:** Accepted

## Context

The architecture is event-driven: the purchase saga, notifications, and
stock-depletion alerts all flow as asynchronous events between services
([ADR-003](ADR-003-choreography.md)). We need a durable, ordered, replayable
message log with consumer-group semantics so that:

- a published event is not lost if a consumer is momentarily down,
- each logical consumer group sees every event once,
- a slow consumer creates backpressure rather than dropping messages.

Redis is **already in the stack** — it is required for caching, rate limiting, and
idempotency regardless of the messaging choice ([ADR-011](ADR-011-cqrs.md),
[ADR-006](ADR-006-api-gateway.md)). The question is whether to add a second piece
of infrastructure (Kafka) for messaging, or use a capability Redis already offers.

## Decision

Use **Redis Streams** as the event bus for all 15 async events.

- Each event type maps to a named stream (`order.placed`, `payment.succeeded`, …),
  catalogued centrally in a shared `StreamNames` constants class.
- Consumers use **consumer groups** so each group processes each event once and
  unacknowledged messages can be reclaimed.
- Every event carries a `BaseEvent` envelope with a unique `eventId`, which
  consumers use as an **idempotency key** — redelivery is safe by design.
- A **dead-letter** example is implemented on `payment.failed` to demonstrate the
  poison-message handling pattern (illustrative tier).

## Alternatives Considered

| Option | Why not (now) |
|---|---|
| **Apache Kafka** | The right tool at high throughput and for long retention / many consumers, but it is a heavyweight addition (brokers, ZooKeeper/KRaft, operational learning curve) unjustified for five services on a single host. Documented as the scale-up path. |
| **RabbitMQ** | Solid broker, but it would still be *new* infrastructure when Redis already satisfies the need, and its routing model is richer than we require. |
| **AWS SNS/SQS / Kinesis** | Cloud-managed and excellent in production, but couples local development to cloud and adds nothing for a self-contained, runnable-anywhere submission. |

## Consequences

**Positive**
- Zero new infrastructure — one fewer thing to run, learn, and operate.
- Durability, ordering per stream, consumer groups, and replay are all native.
- The envelope + `eventId` idempotency pattern is uniform across every consumer.

**Negative / accepted**
- Redis Streams is less battle-tested than Kafka for very high throughput and
  long-term retention; tiered storage and partitioning are weaker. Acceptable at
  this scale.
- Redis now carries more responsibility (bus *and* cache *and* limits *and*
  idempotency) — a Redis outage is broad. Mitigated by the documented split onto
  dedicated infrastructure in production.

**Production path:** Kafka (partitioned topics, longer retention, Schema Registry
for event versioning) behind the same `EventPublisher`/`EventListener` ports — the
[hexagonal](ADR-004-hexagonal-arch.md) boundary means swapping the bus is an
adapter change, not a domain change.

## Related

- [ADR-003](ADR-003-choreography.md) (choreography rides this bus) ·
  [ADR-004](ADR-004-hexagonal-arch.md) (swappable adapter) ·
  [Purchase Saga](../features/purchase-saga.md)
