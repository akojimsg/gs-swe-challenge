# ADR-010 — No service registry (Docker DNS)

**Status:** Accepted

## Context

Services must find each other to make calls — the Gateway to each service, Orders
to Products over gRPC. In larger deployments this is solved with a **service
registry** (Consul, Eureka) plus client-side load balancing. The question is
whether that machinery is warranted *here*: a fixed set of six services on a single
host.

## Decision

Use **no service registry**. Rely on **Docker Compose's built-in DNS** — each
service is reachable at its compose service name (e.g. `http://products:8082`,
`orders:8083`). Addresses are stable and known at compose time.

## Alternatives Considered

| Option | Why not (now) |
|---|---|
| **Consul / Eureka** | Solves dynamic discovery, health-based routing, and client-side load balancing — none of which a static six-service single-host topology needs. Adds a registry to run and register against for zero present benefit. Documented as the Kubernetes/multi-host path. |
| **Hard-coded IPs / ports in config** | Brittle and environment-specific; Docker DNS gives stable *names* without IP coupling. |
| **Kubernetes Services (cluster DNS)** | The natural production answer, but pulls in the whole K8s platform. Out of scope; documented as the path. |

## Consequences

**Positive**
- Zero discovery infrastructure to run or operate.
- Service addresses are stable, readable names in compose and config.
- One fewer moving part to fail or reason about locally.

**Negative / accepted**
- No dynamic discovery, health-aware routing, or client-side load balancing — a
  service is assumed up at its known name. Acceptable for single-host; circuit
  breakers ([ADR-006](ADR-006-api-gateway.md)) handle the "down" case at the edge.
- Does not scale to multiple hosts/replicas as-is. That is precisely the boundary
  at which the documented path activates.

**Production path:** Kubernetes Services + cluster DNS (or Consul) for discovery,
health checking, and load balancing across replicas.

## Related

- [C4 L2 — Containers](../c4/L2-containers.md) · [ADR-006](ADR-006-api-gateway.md)
