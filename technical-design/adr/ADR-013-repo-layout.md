# ADR-013 — Single root Gradle build in a polyglot monorepo

**Status:** Accepted

## Context

The submission is a **polyglot monorepo**: a Java/Gradle backend (a shared library
plus six services), a JavaScript/Vite frontend, plus infrastructure, tests, and
documentation. One repository must host all of it.

The question is where the **Gradle build** sits, and how the non-JVM parts coexist
with it. The instinct that "Gradle can't own the repo root because there's also a
frontend and infra" deserves a real answer rather than an inherited diagram — this
record is that answer.

A layout choice is **load-bearing and awkward to reverse**: it fixes the build
context of every `Dockerfile`, the paths in the `Makefile` and CI workflows, and the
relative links throughout `technical-design/`. Cheap to decide now, expensive to
move later. Hence an ADR.

## Decision

Place a **single Gradle build at the repository root**. `settings.gradle` and the
root `build.gradle` live at the top level and **include only the JVM modules**:

```
include 'shared'
include 'services:gateway'   // …users, products, orders, payments, notifications
```

Everything not in that include list is invisible to Gradle. The non-JVM
directories are **plain siblings**:

```
gs-swe-challenge/
├── settings.gradle  build.gradle  gradlew  gradle/   ← the one Gradle build
├── shared/          services/                          ← its modules
├── frontend/                                           ← self-contained Node/Vite project
├── infra/  tests/  docs/  scripts/                     ← plain directories
└── Makefile  README.md  .github/
```

Gradle does not "own" the root — it merely *roots there* and ignores
`frontend/`, `infra/`, `tests/`, etc. The frontend is built by its own toolchain
(`npm`/Vite) entirely independently. The `Makefile` is the single cross-stack entry
point that drives both.

## Alternatives Considered

| Option | Why not |
|---|---|
| **Nest the backend under `backend/`** (`backend/settings.gradle`, `backend/services/…`) | Visually tidy — one folder per stack — and a fair instinct. But it buys nothing functional: Gradle already ignores sibling dirs, so root-level does not "pollute" the frontend or vice versa. It deviates from the spec and pushes an extra path segment into every Dockerfile build-context, Makefile target, CI path, and `technical-design/` link. Cost without benefit at this scale. |
| **Separate repositories** (backend / frontend / infra) | Defeats the point of a single-repo take-home: one `git clone`, one `make up`, one place to read the design. Polyrepo coordination overhead is unjustified for a system this size. |
| **Gradle composite build / `buildSrc` split per area** | More machinery than six modules in one settings file need. Reserved for genuinely independent builds. |

## Consequences

**Positive**
- Matches the [authoritative spec](../../README.md) and the original layout exactly
  — zero deviation to reconcile.
- One `./gradlew build` covers the whole backend; one `settings.gradle` is the
  module registry.
- The frontend stays fully decoupled with its own toolchain; the two never collide
  because Gradle's scope is the include list, not the directory tree.
- Standard, recognisable polyglot-monorepo shape — low surprise for any reviewer.

**Negative / accepted**
- A handful of Gradle files (`settings.gradle`, `build.gradle`, `gradlew`,
  `gradle/`) sit at the root next to the top-level folders. Cosmetic; the
  `Makefile` is the human entry point, so the root build files are rarely touched
  directly.
- The root build is JVM-centric, so the layout reads as "backend-first" with the
  frontend as a peer folder. Accurate to the system's centre of gravity; not a
  problem.

## Related

- [ADR-004](ADR-004-hexagonal-arch.md) (the module-internal structure each service
  follows) · [C4 L2 — Containers](../c4/L2-containers.md) · the repository
  [directory structure](../../README.md)
