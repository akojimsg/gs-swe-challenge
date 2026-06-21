# Feature — Frontend SPA

**Service:** Frontend (React SPA) · **Tier:** Implemented

The shopper- and admin-facing UI. A single-page app ([ADR-016](../adr/ADR-016-frontend-stack.md))
that consumes **only** the Gateway REST surface ([ADR-006](../adr/ADR-006-api-gateway.md)) —
no direct coupling to services, gRPC, events, or databases. It is how every
acceptance-criterion flow (browse, search, CRUD, CSV import, purchase) is exercised
by a human. Full design package (journeys, layouts, tokens, per-page specs) lives in
`gse-requirement-docs/frontend-design/`.

## Actors & surface

| Actor | Can do |
|---|---|
| **Guest** | Browse, search, filter, view products, build a cart. |
| **Buyer** | + checkout, view own order outcome, profile *(ext)*. |
| **Admin** | + product CRUD, CSV import; order management + logs *(ext)*. |

Public surface (no auth): catalogue, product detail, categories, auth pages.
Everything else needs a session; admin pages require `role=ADMIN` — guarded
client-side **and** re-checked at the Gateway and in-service.

## Routes → API (via Gateway `:8080/api/v1`)

| Route | Page | Access | Primary API |
|---|---|---|---|
| `/` | Home *(ext)* | Public | `GET /products` (featured), `GET /categories` |
| `/products` | Catalogue | Public | `GET /products` (`q,category,minPrice,maxPrice,inStock,page,size,sort`), `GET /categories` |
| `/products/:id` | Product detail | Public | `GET /products/{id}` |
| `/cart` | Cart | Public | client state (Zustand) |
| `/checkout` | Checkout | Buyer | `POST /orders` (+ `Idempotency-Key`) |
| `/order/:id/confirmation` | Confirmation | Buyer | `GET /orders/{id}` (poll) |
| `/login` · `/register` | Auth *(ext)* | Public | `POST /auth/login` · `/auth/register` |
| `/admin/products` | Product list | Admin | `GET /products`, `DELETE /products/{id}` |
| `/admin/products/new` · `/:id/edit` | Product form | Admin | `POST` / `PUT|PATCH /products/{id}` |
| `/admin/import` | CSV import | Admin | `POST /products/import` → `GET /products/import/{id}` |

## The two core journeys

### Purchase (the saga, from the UI side)
Cart → checkout → `POST /orders` with a per-attempt **`Idempotency-Key`** (the
submit button disables on click so a double-click can't create two orders; a retry
returns the original order). Placement returns `PENDING`; the order then advances
**asynchronously** through the [purchase saga](purchase-saga.md). The confirmation
page **polls `GET /orders/{id}`** until terminal:
- `AWAITING_PAYMENT` → a processing state (keep polling),
- `PAID` → success (order #, items, total),
- `FAILED` → a **first-class payment-failed screen** with retry — not a generic
  error, because ~10% of orders fail by design (fake processor).

If stock can't be reserved, placement returns `409` and the UI names the short item
and routes back to the cart — no order is created.

### CSV import (the messy-data path)
Upload a CSV (≤10MB, MIME-guarded client-side) → `202 {importId}` → poll
`GET /products/import/{id}` until `COMPLETED` → render the summary
(`imported / updated / skipped`) and the **per-row error table**
(`row · field · value · reason`). Surfacing the deliberate sample-file errors (XSS
stripped, `"free"`/negative-stock skipped, duplicate SKU updated) is the point of
the feature.

## State strategy

- **Server state** is fetched per-view through the API client; reads reflect the API
  response (e.g. stock is never cached client-side).
- **Client state** is two Zustand stores: `cart` (client-only — there is no cart API;
  persisted so it survives reloads *and* the login detour, per journey J1) and `auth`
  (access token + user; persisted).
- **URL is state** for the catalogue: search/filter/sort/page live in query params so
  results are shareable and back/forward-safe.
- Every page is designed for its full state set — loading, empty (200, not 404),
  error + retry, and the page-specific edges — not just the happy view.

## Auth & token handling

Login/register return an access token (stored in the `auth` store) and set an
**httpOnly refresh cookie** (browser-managed; never read by JS). The API client
sends the Bearer token on every call; on **401** it silently calls
`POST /auth/refresh` once and retries, and on failure clears the session. **429**
surfaces a friendly rate-limit message. Login failures return one generic message
(no account enumeration). Credentials are never logged.

## How it consumes the contract

The SPA binds to the frozen OpenAPI specs (`docs/api/*-openapi.yaml`) — that surface
is its entire backend interface. Two contract notes the UI honours:
- **`categoryId`** (int FK) on product reads — the UI joins `GET /categories` to
  render a label (it does not expect a `category` string).
- **Order status** set is `PENDING → AWAITING_PAYMENT → PAID/FAILED → CANCELLED`
  (the design state machine in [order placement](order-placement.md)); the UI's
  status badges/timeline use this set.

## Packaging

Built to a static `dist/` and served by **nginx**, which proxies `/api` to the
Gateway so the SPA and API share an origin (no CORS; the refresh cookie flows). The
Vite dev server mirrors this with a dev proxy. Runs as a container in the Compose
stack alongside the backend.

## Test coverage

- **Component/unit**: cart math (totals, "Free" at 0), status/stock mapping, the
  idempotency-key + submit-guard logic.
- **E2E (Playwright)**: the AC journeys — purchase (incl. the payment-failed path),
  CSV import with the per-row error report, admin product CRUD.

## Related

- [ADR-016](../adr/ADR-016-frontend-stack.md) (stack choice) ·
  [ADR-006](../adr/ADR-006-api-gateway.md) (the REST edge) ·
  [Order Placement](order-placement.md) · [Purchase Saga](purchase-saga.md) ·
  [CSV Import](csv-import.md) · design package in `gse-requirement-docs/frontend-design/`.
