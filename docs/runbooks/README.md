# Runbooks

Task-focused operational guides for running and exploring the platform locally.
All assume Docker is installed and you are at the repo root.

| Runbook | What it covers |
|---------|----------------|
| [local-stack.md](local-stack.md) | Start, seed, verify, and stop the full stack (`make up` lifecycle) |
| [admin-journey.md](admin-journey.md) | Sign in as an admin and manage the catalogue (CRUD + CSV import) |
| [buyer-journey.md](buyer-journey.md) | Browse, cart, checkout, and watch an order complete |
| [saga-demo.md](saga-demo.md) | Drive the purchase saga end-to-end, including the payment-failure compensation path |

**Start here:** [local-stack.md](local-stack.md) brings the app up; the journey
runbooks assume a running, seeded stack.
