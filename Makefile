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

# Services with a working bootRun entrypoint today, started together by `run-all`.
# Extend as lanes land (notifications, gateway).
RUN_SERVICES := users products orders payments

# Where `run-all` writes per-service logs / pids (outside the repo).
RUN_DIR := /tmp/gsswec-run

.DEFAULT_GOAL := help

.PHONY: help env build test clean up up-obs down restart seed smoke logs ps run-all run-all-stop run-all-status

help: ## List available targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'
	@echo "  \033[36mbuild-<svc>     \033[0m Build one service ($(SERVICES))"
	@echo "  \033[36mtest-<svc>      \033[0m Test one service"
	@echo "  \033[36mrun-<svc>       \033[0m Run one service locally, foreground (bootRun)"

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

# --- Run services locally (bootRun) ------------------------------------------
# These run the JVM services on the host/dev-container via Gradle (NOT in Compose,
# which today only provisions Postgres/Redis/Mailhog). `run-all` backgrounds each
# bootRun with its own log + pid file; `run-all-stop` tears them down cleanly.

run-all: up ## Start all runnable services in the background (RUN_SERVICES)
	@mkdir -p $(RUN_DIR)
	@for svc in $(RUN_SERVICES); do \
		if [ -f $(RUN_DIR)/$$svc.pid ] && kill -0 $$(cat $(RUN_DIR)/$$svc.pid) 2>/dev/null; then \
			echo "  $$svc already running (pid $$(cat $(RUN_DIR)/$$svc.pid))"; \
		else \
			echo "  starting $$svc -> $(RUN_DIR)/$$svc.log"; \
			nohup $(GRADLE) :services:$$svc:bootRun --console=plain \
				> $(RUN_DIR)/$$svc.log 2>&1 & echo $$! > $(RUN_DIR)/$$svc.pid; \
		fi; \
	done
	@echo "Services launching. Watch logs:  tail -f $(RUN_DIR)/<svc>.log"
	@echo "Check health:                    make run-all-status"
	@echo "Stop everything:                 make run-all-stop"

run-all-status: ## Show health of services started by run-all
	@for svc in $(RUN_SERVICES); do \
		port=$$(grep -A2 '^server:' services/$$svc/src/main/resources/application.yml | grep -oE 'port: [0-9]+' | grep -oE '[0-9]+'); \
		if curl -sf -m2 http://localhost:$$port/actuator/health >/dev/null 2>&1; then \
			printf "  \033[32m%-10s UP\033[0m   (:%s)\n" "$$svc" "$$port"; \
		else \
			printf "  \033[31m%-10s DOWN\033[0m (:%s)\n" "$$svc" "$$port"; \
		fi; \
	done

run-all-stop: ## Stop all services started by run-all
	@for svc in $(RUN_SERVICES); do \
		port=$$(grep -A2 '^server:' services/$$svc/src/main/resources/application.yml | grep -oE 'port: [0-9]+' | grep -oE '[0-9]+'); \
		if [ -f $(RUN_DIR)/$$svc.pid ]; then \
			pid=$$(cat $(RUN_DIR)/$$svc.pid); \
			pkill -P $$pid 2>/dev/null; kill $$pid 2>/dev/null; \
			rm -f $(RUN_DIR)/$$svc.pid; \
		fi; \
		if [ -n "$$port" ]; then \
			lsof -ti tcp:$$port 2>/dev/null | xargs -r kill 2>/dev/null || true; \
		fi; \
		echo "  stopped $$svc"; \
	done

# --- Operations --------------------------------------------------------------

seed: ## Load seed data (users, catalogue)
	./scripts/seed-data.sh

smoke: ## Run the post-deploy smoke test
	./tests/smoke/smoke-test.sh
