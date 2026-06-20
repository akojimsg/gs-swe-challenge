# ADR-003 — Saga choreography over orchestration

**Status:** Accepted

## Context

Placing and paying for an order spans three services — Orders, Payments, and
Products — plus Notifications. There is **no distributed transaction**: we cannot
wrap a stock decrement, a payment charge, and an order-status update in one ACID
commit across service boundaries ([ADR-001](ADR-001-database.md) gives each its own
schema). We need a way to keep these consistent that tolerates partial failure —
a payment can fail *after* stock is reserved, and the system must recover without
leaving stock stranded or an order stuck.

This is the classic **Saga** problem: a sequence of local transactions, each with a
**compensating** action that undoes it if a later step fails. The design choice is
*how the saga is coordinated* — by a central **orchestrator**, or by **choreography**
(each service reacts to events and emits its own).

## Decision

Use **choreography**. Services react to domain events and publish their own; no
service holds the saga's control flow.

**Happy path:**
```
order.placed → Payments processes → payment.succeeded
  → Orders sets PAID → Notifications sends receipt
```

**Compensation path:**
```
order.placed → Payments processes → payment.failed
  → Orders sets FAILED
  → Products releases reserved stock   (the compensating action)
  → Notifications notifies the customer of failure
```

Each transition is a local DB transaction plus an event publish. Each consumer is
**idempotent** on `eventId`, so redelivery cannot double-apply (e.g. stock is not
released twice). The full flow, including the state machine and every edge case, is
specified in [Purchase Saga](../features/purchase-saga.md).

## Alternatives Considered

| Option | Why not (now) |
|---|---|
| **Orchestration (a central saga coordinator / state machine service)** | Easier to *read* the flow in one place and to add complex branching, but introduces a single point of failure and a service that must know about every participant — tighter coupling. Overkill for a linear two-outcome saga. |
| **Temporal (durable workflow orchestration)** | Excellent for complex, long-running, observable workflows and the right production answer as flows grow — but a heavyweight dependency (server + workers) for a single, short saga. Documented as the production path. |
| **Two-phase commit / XA** | Not viable across independent service schemas and HTTP/event boundaries; blocks and couples participants. A non-starter for microservices. |

## Consequences

**Positive**
- No single point of failure; no service owns the others' flow.
- Loose coupling — a new reaction (e.g. analytics on `order.paid`) is a new
  subscriber, touching no existing service.
- Naturally resilient: each step retries independently via the bus.

**Negative / accepted**
- The end-to-end flow is **emergent**, not written in one place — harder to follow
  than an orchestrator. Mitigated by the explicit [saga spec](../features/purchase-saga.md)
  and a sequence diagram that documents the choreography.
- Cyclic-dependency risk if events are designed carelessly; avoided by keeping the
  event graph acyclic (see the [event catalogue](../features/purchase-saga.md)).
- Compensation is manual and per-case — there is no generic rollback. This is
  inherent to sagas, not to choreography specifically.

**Production path:** Temporal for orchestration when flows gain branches, timers,
and human-in-the-loop steps. The event contracts stay the same; the coordination
moves into a workflow.

## Related

- [ADR-002](ADR-002-event-bus.md) (the bus it rides on) ·
  [Purchase Saga](../features/purchase-saga.md) ·
  [Order Placement](../features/order-placement.md)
