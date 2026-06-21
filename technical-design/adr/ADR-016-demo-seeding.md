# ADR-016 — Demo data seeding strategy

**Status:** Accepted

## Context

An evaluator's first impression is their first five minutes: `git clone` → run →
open the UI. If the UI is empty, they must bootstrap an admin, products, and
orders by hand before they can assess anything. The application should instead
start from a **non-zero, demonstrably-working state** — an admin account, buyers,
a full catalogue, and (as the services land) orders that have actually flowed
through the saga — so the evaluator can immediately *use* the system and then
perform their own operations (add a product, import a CSV, place an order).

The question is **how** to produce that state without compromising the
architecture, and **when** it should run.

## Decision

### Seed through the service APIs — never raw SQL into service tables

Seeding drives the real endpoints/use-cases (register, login, product create, CSV
import, order placement). It does **not** insert into service-owned tables via
`docker-entrypoint-initdb.d` or direct SQL. Raw inserts would:

- bypass domain rules — passwords wouldn't be BCrypt-hashed (login would fail),
  product/category invariants wouldn't run, and no domain events would fire;
- be unable to produce **orders** at all — an order only exists if the saga
  actually executed (place → reserve stock → pay → notify); a hand-inserted row is
  a hollow, inconsistent fake;
- violate STOSA / [ADR-001](ADR-001-database.md) — each schema is private to its
  service; infra reaching into it couples layers that must stay decoupled;
- be ordering-fragile — init SQL runs before any service has applied its Flyway
  migrations, so the target tables don't yet exist.

Schema bootstrap stays infra's job (the `01-schemas.sql` init). **Data** seeding is
the owning service's job, invoked through its API. Infra owns only the
**orchestration / entry point**.

### `make seed` is idempotent and safe to run anytime

`make seed` runs `scripts/seed-data.sh` after services are healthy. Its contract:

- **Re-runnable with no harm.** It checks current state and acts only where needed:
  creates what's missing, leaves what exists untouched, and repairs partial state
  (e.g. account exists but isn't promoted → promote it).
- **Reports, doesn't fail, on a complete state.** Run after setup is already done,
  it tells the user "already seeded, nothing to do" and exits 0 — never errors,
  never duplicates.
- **Part of initial setup, but explicit.** It is a documented setup step
  (`make up && make seed`), not an unconditional boot hook — auto-seeding every
  environment would contaminate prod and pollute tests. Idempotency makes it safe
  to include in setup flows and to re-run freely.

### Layered, growing with the services

The script is **owned and extended as lanes land**, not written once:

| Layer | Needs | Content |
|---|---|---|
| 1 | Users, Products | admin (`admin@gsswec.com`), buyers, ~90 products + categories via CSV import |
| 2 | Orders, Payments, Notifications | buyers place orders → saga runs → spread of states (PAID, FAILED via the 10% path, CANCELLED) + Mailhog receipts |

Each service lane adds its stage to the script as it merges.

## Alternatives Considered

| Option | Why not |
|---|---|
| **Raw SQL via `docker-entrypoint-initdb.d`** | Bypasses hashing/invariants/events, can't create real orders, violates ADR-001, runs before migrations exist. |
| **Auto-seed on application startup** | Contaminates production and tests; seeding must be opt-in. Idempotent `make seed` gives the convenience without the coupling. |
| **A static SQL dump restored at startup** | Same domain-bypass problems; brittle to schema change; no saga execution. |

## Consequences

**Positive**
- The cloned repo is demo-ready in two commands; the rest state proves the system
  works end-to-end and doubles as a smoke test.
- Seeded data is **consistent by construction** — it went through the same code
  paths as real usage.
- Idempotency makes `make seed` safe in setup scripts, CI, and repeated manual runs.

**Negative / accepted**
- Seeding depends on services being **up and healthy** first (the script polls);
  slower than a SQL dump, but correct.
- The script grows over time and must be kept in step with each service's API —
  accepted, and tracked per lane.
- An interim admin-promotion step is needed until a first-class admin-bootstrap
  exists.

## Related

- Built/tracked in issue #47 (demo seed orchestration). ·
  [ADR-001](ADR-001-database.md) (private per-service schemas) ·
  [ADR-003](ADR-003-choreography.md) (orders exist only via the saga) ·
  the `make seed` target and README quick-start.
