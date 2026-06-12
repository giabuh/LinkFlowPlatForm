# LinkFlowPlatform — root task runner.
# Backend (Gradle + Docker Compose) lives in backend/; frontend (Vite) in frontend/.

BACKEND  := backend
FRONTEND := frontend
COMPOSE  := docker compose
GRADLEW  := ./gradlew

.DEFAULT_GOAL := help

.PHONY: help up down logs restart db-reset \
        run test build be-run be-test be-build \
        fe-install fe-dev fe-build fe-lint dev

help: ## List available targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		sort | awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

## ---- Infrastructure (Postgres + Redis) ----
up: ## Start Postgres + Redis in the background
	cd $(BACKEND) && $(COMPOSE) up -d

down: ## Stop infrastructure containers
	cd $(BACKEND) && $(COMPOSE) down

logs: ## Tail infrastructure logs
	cd $(BACKEND) && $(COMPOSE) logs -f

restart: down up ## Restart infrastructure

db-reset: ## Stop and DELETE all DB/Redis volumes (destroys local data)
	cd $(BACKEND) && $(COMPOSE) down -v

## ---- Backend ----
run: be-run   ## Alias for be-run
test: be-test ## Alias for be-test
build: be-build ## Alias for be-build

be-run: up ## Run the Spring Boot app on :8080 (starts infra first)
	cd $(BACKEND) && $(GRADLEW) bootRun

be-test: ## Run backend tests
	cd $(BACKEND) && $(GRADLEW) test

be-build: ## Compile backend + run all tests
	cd $(BACKEND) && $(GRADLEW) build

## ---- Frontend ----
fe-install: ## Install frontend dependencies
	cd $(FRONTEND) && npm install

fe-dev: ## Run the Vite dev server (:5173, proxies /api to backend)
	cd $(FRONTEND) && npm run dev

fe-build: ## Type-check + production build of the frontend
	cd $(FRONTEND) && npm run build

fe-lint: ## Lint the frontend
	cd $(FRONTEND) && npm run lint

## ---- Combined ----
dev: up ## Start infra (Postgres + Redis), then open the frontend dev server (:5173) in the browser
	cd $(FRONTEND) && npm run dev -- --open
