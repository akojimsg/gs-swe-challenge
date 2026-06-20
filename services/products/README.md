# Products Service

Product catalogue, search, categories, admin CRUD, async CSV import, and the gRPC
stock server that Orders calls on the purchase hot path. Runs on **port 8082**.

## API documentation

- **Swagger UI:** http://localhost:8082/swagger-ui.html (click **Authorize** for
  admin endpoints — use an ADMIN access token from the Users service)
- **OpenAPI (live):** http://localhost:8082/v3/api-docs.yaml
- **OpenAPI (committed):** [`docs/api/products-openapi.yaml`](../../docs/api/products-openapi.yaml)

## Endpoints

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/api/v1/products` | Public | Browse/search (`q`, `category`, `minPrice`, `maxPrice`, `inStock`, `page`, `size`, `sort`) |
| GET | `/api/v1/products/{id}` | Public | Get a product |
| GET | `/api/v1/categories` | Public | List categories |
| POST | `/api/v1/products` | ADMIN | Create a product |
| PUT | `/api/v1/products/{id}` | ADMIN | Replace a product |
| PATCH | `/api/v1/products/{id}` | ADMIN | Partially update a product |
| DELETE | `/api/v1/products/{id}` | ADMIN | Delete a product |
| POST | `/api/v1/products/import` | ADMIN | Async CSV import (see below) |
| GET | `/api/v1/products/import/{id}` | ADMIN | Import job status |

## Search (CQRS read side)

Full-text search uses a PostgreSQL **GIN-indexed `tsvector`** over name +
description ([ADR-001](../../technical-design/adr/ADR-001-database.md)). List/search
results are cached in **Redis with a 60s TTL** and evicted on any write
([ADR-011](../../technical-design/adr/ADR-011-cqrs.md)). **Stock is never relied on
from the cache for reservation** — Orders reads it live over gRPC.

## gRPC stock server

Implements [`stock.proto`](../../docs/api/proto/stock.proto)
([ADR-009](../../technical-design/adr/ADR-009-grpc-internal.md)): atomic
`Reserve` (all-or-nothing, `SELECT … FOR UPDATE`) and a compensating `Release`
for saga rollback. Consumed by Orders.

## CSV import

Async (`202 + importId`, poll for status), row-level validation that never fails
the whole batch, upsert-by-SKU, and robust handling of messy data (currency-string
and `free` prices, negative stock, duplicate SKUs, XSS/SQLi sanitisation, unicode,
empty rows). See [CSV Import](../../technical-design/features/csv-import.md).

## Events published

`product.created`, `product.updated`, `product.deleted`, `product.imported`
(and stock-related events as inventory changes).

## Run locally

```bash
make up              # Postgres + Redis + Mailhog
make run-products    # boots on :8082
```

Admin endpoints require an ADMIN Bearer token — obtain one from the Users service
(`POST /api/v1/auth/login` with an admin account), then **Authorize** in Swagger.

## Design

Hexagonal ([ADR-004](../../technical-design/adr/ADR-004-hexagonal-arch.md)): pure
`domain`, `application` use cases behind ports, `infrastructure`/`api` adapters.
Persistence is PostgreSQL (`products_schema`) via Flyway. Behaviour specs:
[Catalogue & Search](../../technical-design/features/catalogue-and-search.md),
[CSV Import](../../technical-design/features/csv-import.md).
