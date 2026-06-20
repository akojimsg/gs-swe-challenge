# gs-swe-challenge — single entry point for the polyglot monorepo (ADR-013).
# Gradle drives the backend; Docker Compose (infra/) drives the runtime.
# Runs on the host or inside the dev container without modification: java/gradle
# resolve via setup.sh symlinks, and the Docker/Testcontainers environment is
# configured in setup.sh (container) or natively (host) — not encoded here.

COMPOSE      := docker compose --env-file .env -f infra/docker-compose.yml
COMPOSE_OBS  := $(COMPOSE) -f infra/docker-compose.observability.yml
GRADLE       := ./gradlew

# Backend service modules (Gradle paths). Used to generate per-service targets.
SERVICES := users products orders payments notifications gateway

.DEFAULT_GOAL := help

.PHONY: help env build test clean up up-obs down restart seed smoke logs ps

help: ## List available targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'
	@echo "  \033[36mbuild-<svc>     \033[0m Build one service ($(SERVICES))"
	@echo "  \033[36mtest-<svc>      \033[0m Test one service"
	@echo "  \033[36mrun-<svc>       \033[0m Run one service locally (bootRun)"

# --- Environment -------------------------------------------------------------

env: ## Create .env from .env.example if it does not exist
	@test -f .env || (cp .env.example .env && echo "Created .env from .env.example — review secrets before use.")

# --- Build & test (backend) --------------------------------------------------

build: ## Compile and assemble all backend modules
	$(GRADLE) build

test: ## Run all backend tests (unit + Testcontainers integration)
	$(GRADLE) test

clean: ## Remove all Gradle build outputs
	$(GRADLE) clean

# Generate explicit per-service targets (build-users, test-users, run-users, ...).
# Explicit phony targets avoid the "nothing to be done" pitfall of phony pattern rules.
define SERVICE_TARGETS
.PHONY: build-$(1) test-$(1) run-$(1)
build-$(1):
	$$(GRADLE) :services:$(1):build
test-$(1):
	$$(GRADLE) :services:$(1):test
run-$(1): env
	$$(GRADLE) :services:$(1):bootRun
endef
$(foreach svc,$(SERVICES),$(eval $(call SERVICE_TARGETS,$(svc))))

# --- Runtime stack -----------------------------------------------------------

up: env ## Start the core stack (Postgres + Redis + Mailhog + services)
	$(COMPOSE) up --build -d

up-obs: env ## Start the core stack plus the observability stack
	$(COMPOSE_OBS) up --build -d

down: ## Stop and remove all containers (core + observability if present)
	$(COMPOSE) down

restart: down up ## Restart the core stack

ps: ## Show status of the running stack
	$(COMPOSE) ps

logs: ## Tail logs from all running services
	$(COMPOSE) logs -f

# --- Operations --------------------------------------------------------------

seed: ## Load seed data (users, catalogue)
	./scripts/seed-data.sh

smoke: ## Run the post-deploy smoke test
	./tests/smoke/smoke-test.sh
