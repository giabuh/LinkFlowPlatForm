# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

LinkFlow Platform — a URL-shortening + link-analytics backend. Java 21, Spring Boot 3.5, PostgreSQL (Flyway), Redis, Spring Security + JWT. Gradle Kotlin DSL.

**`AGENTS.md` is the product/requirements spec** (domain models, MVP feature list, API surface, security rules, development order). Read it for *what* to build and in what order. This file covers *how* the code is wired. Design specs for in-progress work live in `docs/superpowers/specs/` and plans in `docs/superpowers/plans/`.

## Commands

```bash
docker compose up -d              # start Postgres (5432) + Redis (6379) — required before running/tests
./gradlew bootRun                 # run the app on :8080
./gradlew build                   # compile + run all tests
./gradlew test                    # run all tests
./gradlew test --tests 'com.giabao.linkflow.user.service.UserServiceImplTest'   # single test class
./gradlew test --tests '*UserServiceImplTest.createUser*'                        # single test method
```

DB defaults to `linkflow_db` / `linkflow_user` / `123456`. Override the JWT secret in prod via `APP_JWT_SECRET` and the DB password via `DB_PASSWORD`.

## Architecture

**Package-by-feature modular monolith** under `com.giabao.linkflow`. Each feature package (`auth`, `user`, `link`, …) owns its own `controller / service / repository / entity / dto{request,response} / mapper / specification`. Cross-cutting code lives in `common`, `security`, and `config`. Do **not** create top-level generic `controller`/`service` packages.

### Response envelope (the key cross-cutting mechanism)

Controllers return **plain DTOs / `ResponseEntity<Dto>`**, never a wrapper. Every response is uniformly wrapped into `ApiResponse<T>{ code, message, data }` by `common/response/ApiResponseBodyAdvice` (a `ResponseBodyAdvice`):

- The envelope `code` is the **HTTP status int** (from `ErrorCode`), not a custom code.
- Success message comes from the `@ApiMessage("...")` annotation on the controller method (falls back to a default). Annotate every endpoint with `@ApiMessage`.
- Errors are produced by `GlobalExceptionHandler`, which stashes the `ErrorCode` + message as request attributes (`ERROR_CODE_ATTRIBUTE` / `ERROR_MESSAGE_ATTRIBUTE`) that the advice reads back. Security-filter errors (401/403) are written separately by `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` via `ApiErrorWriter`.

To signal a domain error: `throw new AppException(ErrorCode.CONFLICT, "Email already exists")`. `ErrorCode` is the single enum mapping semantic errors → `HttpStatus`. Add new cases there rather than throwing raw `ResponseStatusException`.

### Auth (`auth` + `security`)

Stateless JWT with **Redis-backed session revocation**:

- Login issues an **access token** (15m, carries `sub`=userId, `role`, `sid`=sessionId) and a **refresh token** (7d, in an HttpOnly cookie scoped to `/api/v1/auth/refresh`). `TokenService` builds/parses both; token type is enforced via a `type` claim.
- `RefreshSessionService` stores `sid → RefreshSession{userId, sha256(refreshToken)}` in Redis with the refresh TTL. Logout / refresh-reuse deletes the session.
- `JwtAuthenticationFilter` authenticates each request from the access token **and checks the session still exists in Redis** — a cryptographically valid access token whose session was revoked is rejected. Any parse failure clears the context (never 500s) so the entry point returns a clean 401.
- Get the authenticated principal via `CurrentUserService.getCurrentUser()` → `CustomUserDetails{id, role}`. Enforce ownership in services (users may only touch their own resources).

### Security routing (`SecurityConfig`)

Stateless, CSRF disabled. Public: `POST /api/v1/auth/{register,login,refresh,logout}` and the redirect endpoint matched by regex `^/[A-Za-z0-9]{6,32}$` (GET). `/users/**` requires `ROLE_ADMIN`. Everything else requires authentication. `@EnableMethodSecurity` is on.

### Persistence conventions

- **Flyway is the schema source of truth** (`src/main/resources/db/migration/V__*.sql`); JPA runs `ddl-auto: validate` and `open-in-view: false`. Never rely on Hibernate to create/alter tables — add a new versioned migration.
- Entities extend `common/entity/BaseEntity` → UUID PK (`GenerationType.UUID`), auditing `createdAt`/`updatedAt` (`@CreatedDate`/`@LastModifiedDate`, enabled by `JpaAuditingConfig`). Store enums as `STRING`.
- Request/response DTOs are **Java records**; map with **MapStruct** mappers (`@Mapper(componentModel="spring")`). Never expose entities directly.

### Pagination / filtering / sorting

List endpoints take a filter DTO extending `common/dto/PageableRequest` (bound via `@ModelAttribute`). Conventions baked into `PageableRequest`: **1-based** `page`, default size 20 / max 100, `sortBy` validated against a per-feature `ALLOWED_SORT_FIELDS` allowlist (unknown fields fall back to default — prevents injection). Build dynamic queries with JPA `Specification`s composed via `common/specification/CommonSpecifications` (`allOf`, `equal`, `containsIgnoreCase`). Return `PageResponse.from(page)`.

## Conventions

- Constructor injection only (`@RequiredArgsConstructor`); thin controllers, logic in services.
- `@Transactional` on writes, `@Transactional(readOnly = true)` on queries.
- No magic strings/numbers — hoist to constants (see existing services for the pattern).
- Services expose an interface + `impl/` package (e.g. `UserService` / `UserServiceImpl`); newer simple services (auth) are concrete `@Service` classes — follow the neighbouring feature's style.
- Tests are heavy unit tests per layer (`*Test` mirroring the main package). Mirror them when adding code.
