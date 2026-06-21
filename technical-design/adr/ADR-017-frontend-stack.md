# ADR-017 — Frontend stack: React + Vite SPA with shadcn/ui, Zustand, Tailwind

**Status:** Accepted

## Context

The challenge requires a UI to exercise the platform — browse/search, cart,
checkout, and the admin product CRUD + CSV import. The frontend is a distinct
container ([C4 L2](../c4/L2-containers.md)) that talks **only** to the Gateway REST
surface; it has no direct coupling to services, gRPC, events, or databases.

The recruiter confirmed any JS framework is acceptable (React, Angular, or other).
We need a stack that (a) is fast to build under the time box, (b) produces an
accessible, themeable UI without hand-rolling primitives, (c) keeps client state
simple (a cart and a session — no server-state framework needed), and (d) packages
cleanly into a static artifact served behind the same origin as `/api`.

## Decision

Build a **React + Vite single-page app (JavaScript)** with:

- **Vite** — dev server + build. Fast HMR, minimal config, static `dist/` output.
- **shadcn/ui** (Radix primitives + Tailwind, components copied into the repo) —
  we own the component code and theme it via the design-system token layer; Radix
  gives accessibility (focus, keyboard, ARIA) for free.
- **Tailwind CSS** — utility styling driven by CSS-variable tokens, so light/dark
  themes work without rebuilding classes ([design system](../../gse-requirement-docs/frontend-design/design-system.md)).
- **Zustand** — minimal client state for the two stateful concerns: `cart`
  (client-only; there is no cart API) and `auth` (access token + user). Persisted
  so the cart survives the login detour.
- **React Router** — route table mapping the sitemap to the four layout shells.
- A **fetch-based API client** bound to the frozen Gateway contract (Bearer token,
  401→refresh→retry once, 429 handling) — no heavy data-fetching dependency.

The SPA is served by **nginx** in production, proxying `/api` to the Gateway
(same-origin → no CORS, the httpOnly refresh cookie flows); the Vite dev proxy
mirrors this in development.

## Alternatives Considered

| Option | Why not |
|---|---|
| **Angular** | Heavier framework with a steeper setup and more ceremony (modules, RxJS) than this UI needs. React + a small state lib matches the surface area better and is faster to deliver under the time box. |
| **Next.js (SSR/RSC)** | SSR/SEO and server components add a Node runtime and deployment surface this internal, auth-gated app doesn't need. A static SPA behind nginx is simpler to build, package, and reason about; SSR is a documented future option, not a requirement. |
| **Plain React (CRA / hand-rolled webpack)** | CRA is effectively unmaintained and slow; hand-rolling a bundler wastes time. Vite is the current default with better DX. |
| **A component kit like MUI/AntD** | Faster to drop in, but harder to theme precisely to the Figma design and you don't own the code. shadcn/ui copies primitives into the repo so the design system is fully under our control. |
| **Redux Toolkit / TanStack Query** | Overkill here — there's no complex server-cache or normalized store. A cart and a token live fine in Zustand; the API client handles the few fetch concerns directly. |
| **TypeScript** | Would add type safety, but the recruiter's stack is JS/ClojureScript and the API contract is already typed by the OpenAPI specs. JSDoc + the contract carry the types; staying JS keeps the build simple. (TS is a low-friction future upgrade.) |

## Consequences

**Positive**
- Fast to build and demo; accessible primitives without writing them.
- Design-system tokens → consistent theming + first-class dark mode.
- Static artifact behind nginx — trivial to containerize and run via Docker Compose
  alongside the backend (same-origin `/api` proxy).
- Clean contract boundary: the SPA binds only to the Gateway OpenAPI surface, so it
  develops independently of backend internals (it is its own build lane).

**Negative / accepted**
- No SSR — irrelevant for an authenticated internal tool; documented as a future
  path if public SEO ever matters.
- JavaScript (not TypeScript) — less compile-time safety; mitigated by the OpenAPI
  contract + JSDoc on the API layer, and TS remains an incremental upgrade.
- shadcn primitives are copied in (not a versioned dependency) — intentional (we own
  the code), but updates are manual.

## Related

- [C4 L2 — Containers](../c4/L2-containers.md) (the SPA container) ·
  [Frontend SPA](../features/frontend-spa.md) (behaviour) ·
  [ADR-006](ADR-006-api-gateway.md) (the REST edge it consumes) ·
  [ADR-014](ADR-014-api-documentation.md) (the OpenAPI contract it binds to)
