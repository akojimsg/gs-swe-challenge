# gs-swe-challenge — single entry point for the polyglot monorepo (ADR-013).
# Gradle drives the backend; Docker Compose (infra/, issue #2) drives the runtime.
# Targets that reference infra/ are wired here but require #2 before they run.

COMPOSE      := docker compose -f infra/docker-compose.yml
COMPOSE_OBS  := $(COMPOSE) -f infra/docker-compose.observability.yml

.DEFAULT_GOAL := help

.PHONY: help build test up up-obs down seed smoke logs

help: ## List available targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

build: ## Compile and assemble all backend modules
	./gradlew build

test: ## Run all backend tests
	./gradlew test

up: ## Start the core stack (services + Postgres + Redis + Mailhog)
	$(COMPOSE) up --build -d

up-obs: ## Start the core stack plus the observability stack
	$(COMPOSE_OBS) up --build -d

down: ## Stop and remove all containers
	$(COMPOSE_OBS) down

seed: ## Load seed data (users, catalogue)
	./scripts/seed-data.sh

smoke: ## Run the post-deploy smoke test
	./tests/smoke/smoke-test.sh

logs: ## Tail logs from all running services
	$(COMPOSE) logs -f
