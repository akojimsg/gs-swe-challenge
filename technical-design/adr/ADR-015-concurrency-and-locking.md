# ADR-015 — Concurrency, locking, and latency model

**Status:** Accepted

## Context

This is a money-adjacent system: a buyer must never be sold stock that isn't
there, and a retried or concurrent request must not corrupt order, inventory, or
payment state. At the same time the platform is **synchronous and blocking** by
construction — Spring MVC (thread-per-request), JDBC, and a blocking gRPC client
on the order hot path. In that model **a thread is a unit of latency-time**: every
millisecond spent blocked on I/O is a millisecond a request thread is unavailable.

Two questions therefore have to be answered deliberately, not by default:

1. **Concurrency control** — how do we keep concurrent writes correct (no
   oversell, no lost update) without serialising more than necessary?
2. **Latency under load** — how do we stop one slow dependency from exhausting
   thread pools and cascading into unrelated failures?

They are the same problem viewed twice: the *duration* a lock or thread is held is
what couples correctness to latency.

## Decision

### 1. Thread-per-request + blocking I/O (not reactive)

Keep the blocking Spring MVC / JDBC stack. Reactive (WebFlux + R2DBC) would free
threads during I/O waits and raise throughput under high-concurrency I/O-bound
load, but it requires a fully non-blocking chain, is materially harder to write
and debug, and does nothing for CPU-bound work or single-request latency. At this
scale, **right-sized pools + bounded waits + isolation** is the correct, simpler
choice. Reactive is documented as a scale-up path, not adopted.

Sizing follows Little's Law — `in-flight = arrival-rate × latency` — so pool sizes
are derived from target latency, and the **Tomcat thread pool and the HikariCP
connection pool are sized in relation to each other** (threads that can't get a
connection just block).

### 2. Locking strategy chosen per contention profile

| Path | Strategy | Why |
|---|---|---|
| **Order placement / stock reservation** | **Pessimistic** — atomic conditional decrement, row-locked (`UPDATE … WHERE stock >= :qty`, equivalent to `SELECT … FOR UPDATE`) | Contention is *expected* (many buyers, same hot SKU) and oversell is unacceptable. Serialise the rare conflict at the row, all-or-nothing across lines. |
| **Admin product update** (read-modify-write) | **Optimistic** — `@Version` column, retry/reject on conflict | Admin edits *rarely* collide; a lock per edit would be pure overhead. Detect the rare lost-update and fail it, rather than serialise every write. |

The principle: **pessimistic where contention is the norm and correctness is
non-negotiable; optimistic where conflict is rare.** Same correctness goal, cost
matched to the access pattern.

### 3. Latency-bounding mechanisms (and where each is applied)

| Mechanism | Applied to | Status |
|---|---|---|
| **Shrink the critical section** | Reservation is a single atomic statement; **no I/O inside the locked transaction** — no event publish, no remote call while a row lock is held. | Implemented |
| **Offload non-response work** | CSV import is `@Async`: the request thread returns `202 + importId` in ms; the long parse runs on a separate executor. | Implemented |
| **Free threads via events** | The purchase saga is choreographed — Orders returns `201` and does **not** hold a thread waiting for payment; Payments reacts on its own pool. | Implemented (ADR-003) |
| **Bulkheads** | The gRPC server dispatches handlers on its own server executor pool (separate from the Netty transport threads) and isolated from the Tomcat request pool — a flood of stock checks can't directly starve REST threads. | Inherent |
| **Deadlines** | A client deadline on the Orders→Products gRPC call caps worst-case thread-hold time and propagates cancellation. | In scope with the Orders build (#11/#44) |
| **Circuit breaker** | Resilience4j trips on a failing downstream so threads fail fast instead of blocking on doomed calls. | Edge done (ADR-006); gRPC-client in scope with the Orders build |

## The worked example — order placement

`Orders.PlaceOrder` → gRPC `StockService.Reserve` → Products:

```
BEGIN
  UPDATE products_schema.products
     SET stock = stock - :qty
   WHERE sku = :sku AND active = true AND stock >= :qty   -- row lock acquired here
COMMIT                                                      -- lock released here
```

- The `WHERE stock >= :qty` makes the decrement **atomic and conditional**: two
  buyers racing for the last unit serialise on the row; exactly one's `UPDATE`
  affects a row, the other affects zero rows → reported as a shortfall. No
  oversell, no application-level locking.
- The lock is held only for the single statement. Nothing else runs in that
  transaction, so the **critical section is microseconds** — the lock-hold time,
  and therefore the latency every concurrent buyer of that SKU waits behind, is
  minimal.
- `CHECK (stock >= 0)` is the last-resort backstop: even a logic bug cannot
  persist negative stock.
- This is why the integration tests run against **real PostgreSQL**
  ([ADR-008](ADR-008-testcontainers.md)) — H2's locking semantics differ, so the
  concurrency guarantee can only be verified against the real engine
  ([Order Placement](../features/order-placement.md) has the concurrent
  last-unit test).

## The extension — where the same concern recurs

The identical *shape* (read current state → compute new state → write) appears in
**`UpdateProduct`** (and in the CSV import's upsert-by-SKU). Two concurrent admin
PATCHes can **lost-update** each other under last-writer-wins.

Here the **right tool is optimistic locking**, not the pessimistic lock used for
reservation, because admin edits rarely collide: add a `@Version` column; on a
stale write Hibernate throws `OptimisticLockException`, which the use case turns
into a `409 Conflict` (or a bounded retry). This deliberately *differs* from the
reservation path — the lock strategy is chosen by the contention profile, not
applied uniformly. (Tracked as a follow-up issue.)

> **Critical subtlety — `@Version` does not guard `stock`.** Reservation is a
> *native* `UPDATE … SET stock = stock - :qty`; it neither loads the JPA entity nor
> bumps `@Version`. So a versioned admin save that *also* wrote `stock` could clobber
> a concurrent decrement and the version check would still pass (the decrementer
> never touched the version). The resolution is **ownership, not more locking**:
> **`stock` is owned solely by reservation/restock and is NOT writable through the
> versioned admin PATCH.** Catalogue edits (`name`, `description`, `price`,
> `category`, `active`) take the optimistic path; stock changes go only through the
> reservation/restock paths. This removes the cross-path race by construction.
> Implementation consequence: the admin update command must **not** accept `stock`.

## Consequences

**Positive**
- Correctness under concurrency is guaranteed at the database, not bolted on in
  application code, and is matched to each path's contention profile.
- Latency is bounded by design: minimal critical sections, offloaded slow work,
  thread isolation, and deadlines/breakers (delivered with the Orders build) keep
  one slow dependency from cascading into pool exhaustion.
- The blocking stack stays simple and debuggable; the reactive option is understood
  and deferred, not ignored.

**Negative / accepted**
- Thread-per-request caps concurrency at pool size; a downstream latency spike
  raises thread demand (Little's Law). Mitigated by deadlines, breakers, and
  bulkheads — not eliminated.
- Pessimistic locking serialises hot-SKU reservations; acceptable because the
  critical section is a single statement.
- Optimistic locking on updates surfaces conflicts to the caller (409/retry)
  rather than hiding them — the correct trade for rare collisions.

## Related

- [ADR-003](ADR-003-choreography.md) (saga frees threads via events) ·
  [ADR-006](ADR-006-api-gateway.md) (circuit breakers, rate limits) ·
  [ADR-008](ADR-008-testcontainers.md) (real-PG concurrency tests) ·
  [ADR-009](ADR-009-grpc-internal.md) (the synchronous hot-path call) ·
  [Order Placement](../features/order-placement.md)
