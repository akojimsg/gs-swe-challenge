# ADR-014 — Code-first OpenAPI via springdoc, per-service spec export

**Status:** Accepted

## Context

Each REST service exposes a customer- or admin-facing HTTP API. For a fintech
client whose job description explicitly values API design, the API **contract**
must be documented — both as a browsable reference for consumers (the frontend,
reviewers) and as a versioned artifact in the repository.

The handoff directory structure reserves `docs/api/openapi.yaml`, and issue #20
lists an OpenAPI spec as an outstanding deliverable. The question is *how* the spec
is produced and kept truthful as the code evolves.

Two broad strategies exist: **spec-first** (hand-author the YAML as the contract,
generate or hand-write code to match) and **code-first** (derive the spec from the
running application's controllers and DTOs).

## Decision

Use **code-first** documentation with **springdoc-openapi** in every REST service:

- Add `springdoc-openapi-starter-webmvc-ui` to each service. This exposes a live
  `/v3/api-docs` (JSON), `/v3/api-docs.yaml`, and an interactive `/swagger-ui.html`
  generated from the actual controllers, DTOs, and bean-validation annotations.
- Annotate only where the contract needs nuance the code cannot convey — an
  `@Operation` summary, and `@ApiResponse` entries for the error statuses (e.g. the
  409/401 bodies) that no happy-path return type expresses.
- **Export a static `docs/api/<service>-openapi.yaml`** per service (boot the
  service, fetch `/v3/api-docs.yaml`, commit the file) so the contract is a durable,
  reviewable artifact — not only a runtime endpoint.
- The springdoc/Swagger paths are permitted in each service's security config.

The **gateway** ([ADR-006](ADR-006-api-gateway.md)) is the documented path for
*aggregating* the per-service specs into one published surface once it exists
(#15); until then, per-service files are the only thing that can be accurate.

## Alternatives Considered

| Option | Why not (here) |
|---|---|
| **Spec-first (hand-authored YAML)** | A purer "design-first" story, and valuable when many teams negotiate a contract before code. But it creates a second source of truth that must be manually kept in lockstep with the code — guaranteed drift on a fast-moving solo build. We are already code-first on the first service; switching would mean maintaining the contract twice. |
| **No machine-readable spec (prose docs only)** | Fails the deliverable and the JD signal; gives consumers nothing to generate clients or run contract checks against. |
| **Generate code from a spec (e.g. openapi-generator)** | Inverts control toward spec-first with heavy build machinery; unjustified for hand-written hexagonal controllers. |

## Consequences

**Positive**
- The spec **cannot drift** from the implementation — it is generated from it. A
  changed DTO or endpoint changes the spec automatically.
- Near-zero authoring cost: the controllers and validation we already write *are*
  the spec; annotations add only the error-contract nuance.
- Interactive Swagger UI per service aids local development and review.
- A committed `docs/api/*.yaml` per service gives a reviewable, diffable contract.

**Negative / accepted**
- A small runtime dependency in each service (springdoc) and a few doc paths to
  permit in security config. Trivial.
- The exported YAML is a **snapshot** — it must be re-exported when the API
  changes. Mitigated by making the export a documented step (and, later, a CI
  step that regenerates and diffs it).
- Code-first means the contract follows the code rather than leading it; acceptable
  for this single-author build where the design docs already lead.

**Future path:** gateway-level aggregation of all service specs into one published
OpenAPI document; a CI check that re-exports each spec and fails on uncommitted
drift; optional Spring Cloud Contract for consumer-driven contract testing (already
noted as illustrative in the test strategy).

## Related

- [ADR-006](ADR-006-api-gateway.md) (gateway aggregation path) ·
  [Auth & RBAC](../features/auth-and-rbac.md) · issue #20 (docs deliverable)
