# ADR-009 — gRPC for the Orders → Products internal call

**Status:** Accepted

## Context

Order placement has exactly one point where a service genuinely needs a *synchronous*
answer from another before it can proceed: Orders must confirm — and reserve —
stock with Products before it can create an order ([Order Placement](../features/order-placement.md)).
This call is on the **hot path** of every purchase and must be fast and unambiguous.

Everything else between services is asynchronous and goes over the event bus
([ADR-002](ADR-002-event-bus.md)). This decision is *only* about that one internal
synchronous hop.

## Decision

Use **gRPC** for the Orders → Products stock-check/reserve call.

- A `stock.proto` contract defines the service and messages; both sides generate
  typed stubs from it — the contract is the single source of truth.
- Products hosts the **gRPC server**; Orders is the **gRPC client**, behind a
  `StockClient` outbound port ([ADR-004](ADR-004-hexagonal-arch.md)) so the
  transport is swappable.
- External/browser traffic stays **REST** ([ADR-006](ADR-006-api-gateway.md)); gRPC
  is internal-only.

## Alternatives Considered

| Option | Why not |
|---|---|
| **Internal REST/JSON** | Works, but text serialization is heavier on a hot path, and the contract is informal (no generated stubs, drift risk). gRPC's binary framing and generated, strongly-typed contract fit a high-frequency internal call better. |
| **Make it asynchronous (event-based reservation)** | Stock reservation needs a *synchronous yes/no* — Orders cannot create an order on the hope that stock exists. Forcing it async would mean optimistic creation plus a compensation for "actually, no stock," adding complexity to the common case. The saga already handles the *payment* outcome asynchronously; reservation is the one place sync is correct. |
| **gRPC everywhere (including the edge)** | Poor browser fit; needs a proxy (grpc-web) and loses REST's cacheability and ubiquity. Right tool, wrong boundary. |

## Consequences

**Positive**
- Low-latency, binary, strongly-typed call on the purchase hot path.
- The `.proto` is an enforced contract — client and server cannot silently diverge.
- Demonstrates deliberate protocol selection: sync where needed, async everywhere
  else.

**Negative / accepted**
- A second internal protocol alongside REST and events — more to understand and
  operate. Justified by being confined to one well-defined call behind a port.
- gRPC tooling (proto compilation, codegen) adds build complexity. Contained in the
  Gradle build.
- A synchronous dependency means Orders is coupled to Products' availability at
  placement time. Mitigated by client-side timeouts and the option of a circuit
  breaker; a hard failure here correctly *prevents* an unbacked order rather than
  creating one.

## Related

- [ADR-002](ADR-002-event-bus.md) (async everywhere else) ·
  [Order Placement](../features/order-placement.md) ·
  [C4 L2 — Containers](../c4/L2-containers.md)
