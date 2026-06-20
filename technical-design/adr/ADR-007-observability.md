# ADR-007 — Grafana stack (Prometheus + Loki + Jaeger) over Datadog

**Status:** Accepted

## Context

An event-driven system with a saga spanning four services is hard to debug without
**correlated** observability: when an order is stuck, an engineer must follow one
request across services through metrics, logs, *and* traces. The job description
explicitly values observability and on-call operability.

We need the three pillars — metrics, logs, traces — correlated by a shared
`traceId`, runnable locally at zero cost, and without locking the code to a vendor.

## Decision

Use the open-source **Grafana stack**, with **Micrometer** and **OpenTelemetry** as
the vendor-neutral instrumentation layer:

- **Metrics** — Micrometer → **Prometheus** → Grafana dashboards.
- **Logs** — structured **JSON** (Logback + logstash encoder) → **Loki** → Grafana.
- **Traces** — **OpenTelemetry** (Java agent auto-instruments Spring Boot) →
  **Jaeger**.
- **Correlation** — `traceId` (and `userId` where present) injected into MDC at the
  Gateway ([ADR-006](ADR-006-api-gateway.md)) and propagated, so every log line and
  span for one request shares an ID.

Discipline that prevents the common failure modes:
- Every log line is JSON with `timestamp, level, event, traceId, userId,
  duration_ms`.
- Metrics carry a counter **and** a histogram per endpoint.
- **Never** use `userId`/`orderId` as metric tags — unbounded cardinality would
  blow up Prometheus. High-cardinality identifiers belong in logs/traces, not
  metric labels.

## Alternatives Considered

| Option | Why not (now) |
|---|---|
| **Datadog** | Excellent managed product, but paid and cloud-coupled — wrong for a self-contained, zero-cost, runs-anywhere submission. Crucially, because instrumentation is **Micrometer + OTEL**, switching to Datadog is a **config/exporter change**, not a code change. |
| **ELK (Elasticsearch/Logstash/Kibana)** | Heavier to run locally than Loki for log volume of this scale; Loki's label-based model pairs naturally with Prometheus and Grafana. |
| **Hand-rolled logging/metrics** | Reinvents correlation and dashboards badly; ignores mature, free tooling. |

## Consequences

**Positive**
- Full three-pillar observability, correlated, running locally for free.
- Vendor-neutral: Micrometer/OTEL means the backend (Datadog, Grafana Cloud, etc.)
  is swappable by configuration.
- The cardinality and structured-log rules prevent the two most common
  observability outages (metric explosion, ungreppable logs).

**Negative / accepted**
- Four observability containers (Prometheus, Loki, Jaeger, Grafana) add local
  resource cost. Isolated in a separate `docker-compose.observability.yml` so the
  core system can run without them.
- Self-hosted means no managed retention/alerting SLAs — fine locally; production
  uses managed backends via the same exporters.

## Related

- [C4 L2 — Containers](../c4/L2-containers.md) · [ADR-006](ADR-006-api-gateway.md)
  (where `traceId` is injected)
