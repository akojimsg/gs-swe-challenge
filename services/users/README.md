# Users Service

Identity and access control for the platform: registration, login, JWT issuance
and refresh, and user/role management. Runs on **port 8081**.

## API documentation

- **Swagger UI:** http://localhost:8081/swagger-ui.html (interactive; click
  **Authorize** to use a Bearer token on protected routes)
- **OpenAPI (live):** http://localhost:8081/v3/api-docs.yaml
- **OpenAPI (committed):** [`docs/api/users-openapi.yaml`](../../docs/api/users-openapi.yaml)

## Endpoints

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Register a BUYER; returns access token + refresh cookie |
| POST | `/api/v1/auth/login` | Public | Authenticate; returns access token + refresh cookie |
| POST | `/api/v1/auth/refresh` | Cookie | Rotate the refresh token, issue a new access token |
| POST | `/api/v1/auth/logout` | Cookie | Revoke the refresh token (204) |
| GET | `/api/v1/users/me` | Authenticated | Current user's profile |
| PATCH | `/api/v1/users/me` | Authenticated | Update own profile |
| GET | `/api/v1/users` | ADMIN | List users (paginated) |
| GET | `/api/v1/users/{id}` | ADMIN | Get a user by id |
| PATCH | `/api/v1/users/{id}/role` | ADMIN | Change a user's role (emits `user.role_changed`) |

## Tokens

- **Access token** — JWT (HS256), 15 min, claims `sub`/`email`/`role`. Stateless;
  verified by signature, no DB lookup.
- **Refresh token** — opaque UUID, 7 days, SHA-256 **hashed at rest**, delivered as
  an httpOnly cookie. Rotated on every refresh; revocable on logout.

## Events published

- `user.registered` — on registration
- `user.role_changed` — on an admin role change

## Run locally

```bash
make up            # Postgres + Redis + Mailhog
make run-users     # boots on :8081
```

Then register/login via Swagger to obtain a token. The seeded `admin@gsswec.com`
account is not yet provisioned (see issue #36); until then, a new user is created
as a BUYER and must be promoted by an existing admin (or via a local DB update).

## Design

Hexagonal (ports & adapters) per
[ADR-004](../../technical-design/adr/ADR-004-hexagonal-arch.md): pure `domain`,
`application` use cases behind ports, `infrastructure`/`api` adapters. Persistence
is PostgreSQL (`users_schema`) via Flyway; auth/RBAC behaviour is specified in
[Auth & RBAC](../../technical-design/features/auth-and-rbac.md).
