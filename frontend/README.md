# Frontend — United Deals SPA

React + Vite single-page app for `gs-swe-challenge`. Talks **only** to the Gateway
REST surface (`/api/v1`). Stack & rationale: [ADR-017](../technical-design/adr/ADR-017-frontend-stack.md).
Behaviour: [frontend-spa feature doc](../technical-design/features/frontend-spa.md).

> **State of this scaffold:** the app is **build-ready**, not visually finished.
> The data layer, stores, routing, guards, the four shells, and an annotated stub
> for every page are wired against the frozen contract. **Screens are implemented
> by an agent with Figma MCP** (see "Build handoff" below). `npm run build` is green.

## Stack

React 18 · Vite 5 · React Router · Zustand (`cart`, `auth`) · Tailwind +
shadcn/ui-style primitives · `@fontsource/inter` + `lato`. JavaScript (no TS).

## Run

```bash
npm install
cp .env.example .env.local        # adjust VITE_PROXY_TARGET to your gateway
npm run dev                        # http://localhost:5173, proxies /api → gateway
npm run build                     # static dist/
npm run lint
```

The dev server proxies `/api` to `VITE_PROXY_TARGET` (default
`http://localhost:8110`, the frontend lane's gateway host port). With the backend
up (`make up` + services), the **catalogue** and **admin** pages fetch real data
today; **checkout**'s PAID/FAILED needs the saga service (the confirmation page
polls and degrades gracefully until it lands).

## Layout

```
src/
├── api/        client.js (fetch wrapper: Bearer, 401→refresh→retry, 429) + products/orders/auth
├── store/      cart.js, auth.js (Zustand, persisted)
├── lib/        cn.js, constants.js (status/stock maps, thresholds), format.js
├── styles/     tokens.css (design-system CSS vars, light + dark)
├── components/
│   ├── layout/ StorefrontShell, CheckoutShell, AuthShell
│   ├── admin/  AdminShell (+ domain components: table, form, import widgets)
│   ├── ui/     Button, Badge (shadcn-style primitives)
│   └── common/ States (Spinner/Empty/Error), Guards (RequireAuth/RequireAdmin)
└── pages/      Catalogue, ProductDetail, Cart, Checkout, OrderConfirmation,
                Login, Register, Home, NotFound, Forbidden, admin/*
```

## Build handoff — implementing a screen (needs Figma MCP)

Every page is an annotated stub: its **file header** names the Figma node id, the
spec, the shell, the API calls, and the state set it must cover. To implement one:

1. Open the page's spec in `gse-requirement-docs/frontend-design/specs/`.
2. Pull its frame via **Figma MCP** from file `eK34KAtvQCGRw6QPc1jzPU` ("United Deals").
3. Replace the stub's render with the real UI built **only** from the design-system
   tokens + shadcn/shared components. The data layer (API, stores, loading/error
   handling) is already wired — implement the visual against it, don't rewire it.
4. Order: **P1 core** (catalogue, product detail, cart, checkout, confirmation,
   admin list/form/import) first — those are the graded acceptance criteria — then
   **P2** (auth, home).

| Page | Route | Figma node | Spec |
|---|---|---|---|
| Catalogue | `/products` | `1:39935` | `specs/catalogue.md` |
| Product detail | `/products/:id` | `1:17310` | `specs/product-detail.md` |
| Cart | `/cart` | `1:12102` | `specs/cart.md` |
| Checkout | `/checkout` | `1:12229`/`1:12347` | `specs/checkout.md` |
| Order confirmation | `/order/:id/confirmation` | `1:12481`/`1:12670` | `specs/order-confirmation.md` |
| Auth | `/login`·`/register` | `1:6340` | `specs/auth.md` |
| Home | `/` | `1:21021` | `specs/home.md` |
| Admin products | `/admin/products` | admin shell | `specs/admin-products.md` |
| Admin product form | `/admin/products/new`·`/:id/edit` | admin shell | `specs/admin-product-form.md` |
| Admin CSV import | `/admin/import` | admin shell | `specs/admin-csv-import.md` |

## Contract notes (truth — overrides older spec examples)

- Product reads return **`categoryId`** (int) — join `GET /api/v1/categories` for a
  label; there is no `category` string.
- Order status set: **`PENDING / AWAITING_PAYMENT / PAID / FAILED / CANCELLED`**
  (`src/lib/constants.js`).
- Checkout sends a per-attempt **`Idempotency-Key`**; the submit button disables on
  click so a double-submit can't create two orders.

## Docker

`Dockerfile` builds the SPA and serves it via nginx with an `/api` → Gateway proxy
(`nginx.conf`; `$GATEWAY_URL` templated at start). Same-origin in prod (no CORS; the
httpOnly refresh cookie flows).
