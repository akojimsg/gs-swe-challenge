# Runbook: Local stack lifecycle

Bring the whole application up, seed it, verify it, and tear it down. The only
prerequisite is **Docker** — every service is built inside Docker, so no host
JDK, Gradle, or Node is needed.

## Start

```bash
git clone https://github.com/akojimsg/gs-swe-challenge.git
cd gs-swe-challenge
make up      # build images + start the full stack (detached)
make seed    # load the sample catalogue + demo users
```

`make up` wraps `docker compose up --build -d`. The first run builds the service
images and may take a few minutes; later runs are cached.

## Verify

```bash
make ps                       # all containers should be "healthy"
curl localhost:8080/actuator/health     # gateway → 200
curl localhost:3000/                     # frontend SPA → 200
curl localhost:3000/api/v1/products      # SPA proxies /api to the gateway → 200
```

Open the storefront at **http://localhost:3000**.

## Service URLs

| Service | URL |
|---------|-----|
| Frontend (SPA) | http://localhost:3000 |
| Gateway (API entry point) | http://localhost:8080 |
| Users | http://localhost:8081 |
| Products | http://localhost:8082 |
| Orders | http://localhost:8083 |
| Payments | http://localhost:8084 |
| Notifications | http://localhost:8085 |
| Mailhog (captured email) | http://localhost:8025 |

The browser only needs the **frontend** (`:3000`); it reaches every API through
the gateway. The individual service ports are exposed for inspection/debugging.

## Seed credentials

```
admin@gsswec.com / admin123   [ADMIN]
buyer@gsswec.com / buyer123   [BUYER]
```

## Common operations

```bash
make logs     # tail all service logs
make ps       # stack status
make down     # stop and remove the stack (data volumes persist)
```

## Notes

- `make up` starts the entire app — gateway, frontend, the five backend
  services, and their datastores (Postgres, Redis, Mailhog).
- The stack is reachable end-to-end through the gateway; the SPA at `:3000`
  is the intended entry point.
- To start completely fresh (drop seeded data), `make down` removes containers;
  remove the named volumes if you also want to clear the database.
