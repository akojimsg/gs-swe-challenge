# ADR-004 — Hexagonal architecture (ports & adapters) per service

**Status:** Accepted

## Context

The stated end goal for the client is a migration toward **Clojure/EDA**. Whatever
we build in Java should not entangle the business logic with the framework, so that
the *domain* — the part that holds real value — can survive a change of framework,
datastore, or even language.

Independently, we want fast, deterministic unit tests of business rules without
booting Spring or a database, and the freedom to swap the event bus
([ADR-002](ADR-002-event-bus.md)) or persistence ([ADR-001](ADR-001-database.md))
without rewriting use cases.

## Decision

Every service uses **hexagonal architecture** (ports & adapters) with four layers
and a strict inward-only dependency rule:

```
api / infrastructure  →  application  →  domain
        (adapters)         (use cases)    (pure)
```

- **`domain`** — records, value objects, and rules. **Zero framework imports.**
- **`application`** — use cases and the **ports** (interfaces) they need.
- **`infrastructure`** — adapters that *implement* outbound ports (JPA, Redis,
  gRPC client) and Spring wiring.
- **`api`** — inbound adapters (REST controllers, gRPC endpoints, event listeners).

Domain entities and JPA entities are **separate types**; the persistence adapter
maps between them. The full layout and a request trace are in
[C4 L4 — Code](../c4/L4-code.md).

## Alternatives Considered

| Option | Why not |
|---|---|
| **Layered / N-tier (controller → service → repository, Spring throughout)** | Familiar and fast to start, but couples the domain to Spring/JPA annotations, making the Clojure migration and framework-free testing far harder. The thing we most want to protect (the domain) would be the most entangled. |
| **Classic Clean Architecture** | Essentially the same dependency rule with more ceremony/naming layers; hexagonal expresses the ports/adapters idea more directly for a service of this size. |
| **Anemic domain + transaction scripts** | Simplest, but pushes all logic into service classes and loses the portable, testable domain core — the opposite of the migration goal. |

## Consequences

**Positive**
- The domain is **pure** — unit-testable with plain JUnit in milliseconds, no
  container, no DB.
- Technology choices are swappable at the adapter edge: changing the bus or DB does
  not touch use cases or domain.
- Provides a **clean Clojure migration path** — the domain logic is already
  framework-free and built from immutable values
  ([ADR-005](ADR-005-functional-java.md)).

**Negative / accepted**
- More indirection and more types (domain entity *and* JPA entity, port interface
  *and* adapter) than a flat layered design — more files, more mapping. Justified by
  the testability and portability gains; the cost is paid once per service in a
  consistent, learnable pattern.
- Risk of over-applying it to trivial services. Mitigated by letting thin services
  (Gateway, Notifications) collapse unused layers — see the deviations in
  [C4 L4](../c4/L4-code.md#where-this-layout-is-bent).

## Related

- [C4 L4 — Code](../c4/L4-code.md) · [ADR-005](ADR-005-functional-java.md) ·
  [ADR-008](ADR-008-testcontainers.md) (what the pure domain lets us test cheaply)
