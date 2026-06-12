# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

This is a monorepo for **LinkFlowPlatform**, a production-style URL-shortening + link-analytics SaaS.

- `backend/` — the Spring Boot app. It has its own detailed `backend/CLAUDE.md` covering how the code is wired (response envelope, JWT/Redis auth, persistence, pagination). Read it before touching backend code.
- `frontend/` — Vite + React 19 + TypeScript SPA (TanStack Router/Query, React Hook Form, Zod, Zustand, Tailwind v4, Recharts). Scaffold only so far: a typed envelope API client (`src/lib/api.ts`), in-memory auth-token store (`src/stores/auth.ts`), code-based router (`src/router.tsx`), and Home/Health pages. Dev server (:5173) proxies `/api` and `/actuator` to the backend on :8080. **All UI must follow `DESIGN.md`** (see below).
- `docs/` — `architecture.md`, plus `superpowers/specs/` (design specs) and `superpowers/plans/` (implementation plans) for in-progress features.
- `README.md`, `ROADMAP.md`, `AGENTS.md` — product/requirements docs (see below).
- `DESIGN.md` — the **frontend design system** ("Micro": light theme, blue→teal hero gradient, warm off-white canvas, Ink Black text, Azure Action `#518bdb` CTA). It is the visual source of truth: design tokens, type scale, component specs, and a ready-to-paste Tailwind v4 `@theme` block. Drop that block into `frontend/src/index.css` and build UI from its tokens/components rather than ad-hoc colors — do not introduce accents outside its named palette.
- `Makefile` — root task runner that delegates to both modules (`make help` to list). `make up/down/db-reset` for infra, `make run/test/build` for backend, `make fe-dev/fe-build` for frontend, `make dev` to run both.

## Source-of-truth order

When a task's *intent* is unclear, resolve it against these in order (later loses to earlier on conflict):

1. `ROADMAP.md` — the authoritative staged build plan, MVP scope, tech choices, and per-stage acceptance criteria. **Identify which stage a requested feature belongs to and do not implement unrelated future stages.**
2. `AGENTS.md` — coding rules and module conventions (what to build, what to avoid).
3. `README.md` — product overview.
4. `backend/CLAUDE.md` — *how* the existing code is actually wired.
5. Source code and tests.

If a request conflicts with `ROADMAP.md`, follow the roadmap unless the user explicitly overrides.

## Commands

Run from `backend/`:

```bash
docker compose up -d              # Postgres (5432) + Redis (6379) — required before bootRun/tests
./gradlew bootRun                 # run on :8080
./gradlew build                   # compile + all tests
./gradlew test                    # all tests
./gradlew test --tests 'com.giabao.linkflow.user.service.UserServiceImplTest'   # single class
./gradlew test --tests '*UserServiceImplTest.createUser*'                        # single method
```

## Big-picture architecture

**Package-by-feature modular monolith** under `com.giabao.linkflow` — one deployable Spring Boot service, business logic split by feature package (`auth`, `user`, `link`, `redirect`), each owning its own `controller/service/repository/entity/dto/mapper/specification`. Cross-cutting code lives only in `common`, `security`, `config`. Never add top-level generic `controller`/`service` packages, and never extract a microservice. See `backend/CLAUDE.md` for the response-envelope, auth, persistence, and pagination mechanisms that span these packages.

Three layers must stay separated (enforced in review): controllers handle HTTP I/O only, services own business rules + ownership checks + transactions, repositories only persist. Controllers never return entities — always response DTO records mapped via MapStruct.

## Current implementation stage

Implemented packages: `common`, `config`, `security`, `auth`, `user`, `link`, `redirect` — i.e. roughly through ROADMAP Stage 5 (auth + JWT + Redis refresh sessions, user/link CRUD with ownership, public redirect). Only `V1__init_schema.sql` exists. **Not yet built:** Redis redirect cache, Kafka click events, analytics, QR code, admin, rate limiting. Do not start Kafka/analytics/frontend before the link + redirect core is stable (roadmap rule).

## Non-negotiable rules (from ROADMAP/AGENTS)

- **Flyway is the schema source of truth.** Every schema change is a new versioned migration in `backend/src/main/resources/db/migration/`; never edit an applied migration, never rely on Hibernate `ddl-auto` to alter tables (it runs `validate`).
- **Ownership is a security boundary, not a filter.** Never trust a `userId` from the request body — resolve the current user from the security context (`CurrentUserService`) and verify it owns the resource before any read/mutation. Ownership bugs are treated as serious bugs.
- **URL validation rejects unsafe protocols** (`javascript:`, `file:`, `data:`) — only `http`/`https` are allowed for stored link destinations.
- Domain errors go through `AppException(ErrorCode.…, msg)` + the global handler, never raw `RuntimeException`/`ResponseStatusException` or stack traces to clients.
- Don't change package names, group ID, Java version, or Spring Boot version (Java 21 / Spring Boot 3.5.x) without explicit approval.

## Conventions

- Make the smallest correct change for the task's stage; avoid large unrelated refactors.
- Mirror the heavy per-layer unit tests (`*Test` mirroring the main package) when adding code; security/ownership tests are mandatory for ownership-sensitive features.
- Commit style is conventional with a scope: `feat(auth):`, `fix(link):`, `test(security):`, `docs(readme):`. Don't mix unrelated changes in one commit.
