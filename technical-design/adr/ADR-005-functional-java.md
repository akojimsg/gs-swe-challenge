# ADR-005 — Functional Java style (immutable records, no setters)

**Status:** Accepted

## Context

The domain layer is meant to be portable to Clojure ([ADR-004](ADR-004-hexagonal-arch.md)),
a language built on immutable data and pure functions. Beyond migration, immutable
domain objects eliminate a whole class of bugs — aliasing, accidental mutation,
unsafe sharing across threads — which matters in an event-driven system where the
same payload may be handled concurrently.

Java 21 gives us the tools to write in this style natively: records, sealed types,
pattern matching, and a mature Stream API.

## Decision

Write the `domain` and `application` layers in a **functional Java style**:

- **All domain objects are `record`s** — immutable, value-semantic.
- **No setters anywhere** in domain/application. A state change returns a *new*
  value: `order.withStatus(PAID)` rather than `order.setStatus(PAID)`.
- **`Optional` over `null`** at API boundaries; absence is explicit.
- **Stream API over imperative loops** for collection transformations.
- **Unmodifiable collections** returned across domain boundaries.
- **Constructor injection only** — no field `@Autowired`; dependencies explicit.

These are enforced by convention and code review (and partly by `record`
immutability itself).

## Alternatives Considered

| Option | Why not |
|---|---|
| **Idiomatic mutable OO Java (JavaBeans, setters, mutable entities)** | The default, and fine for many systems — but it works against the Clojure migration and reintroduces the mutation/aliasing risks we want gone. |
| **Lombok `@Data`/`@Builder`** | Reduces boilerplate but encourages mutability (`@Data` generates setters) and adds annotation-processing magic. Records cover the immutable-value need without a dependency. |
| **A functional library (Vavr)** | Brings `Either`, persistent collections, etc., but adds a dependency and a second idiom on top of the JDK. Java 21 + `Optional` + Streams is enough for this scope. |

## Consequences

**Positive**
- Domain values are immutable and thread-safe by construction — safe to pass
  through events and across consumer threads.
- The code reads close to the target Clojure idiom, making the migration a
  translation rather than a redesign.
- Records give `equals`/`hashCode`/`toString` for free, which simplifies testing
  and event deduplication.

**Negative / accepted**
- "Wither" methods and rebuilding records on each change are more verbose than a
  setter and can create short-lived garbage. Negligible at this scale; the safety
  is worth it.
- The style is enforced by review, not the compiler — a setter *could* be added.
  Caught in review; the absence of mutable fields in records makes violations
  conspicuous.

## Related

- [ADR-004](ADR-004-hexagonal-arch.md) · [C4 L4 — Code](../c4/L4-code.md#functional-java-conventions)
