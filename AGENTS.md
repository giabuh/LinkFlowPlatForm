# AGENTS.md

> Implementation guide for AI coding agents such as Claude, Codex or other automated development assistants working on LinkFlowPlatform.

This file tells agents how to understand the project, what to build, what to avoid, and how to keep the codebase consistent.

---

## 1. Agent Mission

You are helping build LinkFlowPlatform, a production-style link management, URL shortening, and click analytics platform.

The project must be strong enough for:

- Real users.
- A developer CV.
- Technical interviews.
- Backend system design discussion.
- Future extension into a SaaS-style platform.

Do not treat this project as a toy CRUD application.

---

## 2. Source of Truth Order

Before implementing any feature, read these files in order:

1. `roadmap.md`
2. `AGENTS.md`
3. `README.md`
4. Related files in `docs/`
5. Existing source code
6. Existing tests

If a task conflicts with `roadmap.md`, follow `roadmap.md` unless the user explicitly says otherwise.

---

## 3. Current Architecture Direction

Use a modular monolith.

This means:

- One deployable Spring Boot application.
- Business logic separated by modules.
- Clean module boundaries.
- No premature microservices.
- No random cross-module coupling.

Recommended package structure:

```text
com.giabao.linkflow
├── common
├── config
├── security
├── auth
├── user
├── link
├── redirect
├── analytics
├── qrcode
├── admin
└── audit
```

Each business module should normally contain:

```text
controller/
dto/
entity/
mapper/
repository/
service/
specification/    # only when filtering/searching is needed
```

Do not create folders that are not useful yet.

---

## 4. Technology Baseline

Use the following default stack unless the user changes it:

- Java 21
- Spring Boot 3.5.x
- Gradle Kotlin DSL
- PostgreSQL
- Flyway
- Spring Data JPA
- Spring Security
- JWT
- Redis
- Kafka
- Docker Compose
- JUnit 5
- Testcontainers when integration testing infrastructure is required
- MapStruct for DTO mapping
- Lombok where it improves readability

Do not upgrade major framework versions without user approval.

---

## 5. Development Philosophy

Prioritize in this order:

1. Correctness
2. Security
3. Maintainability
4. Observability
5. Performance
6. Developer experience

Use clean code principles:

- Small classes.
- Clear method names.
- No duplicated business rules.
- No large controller methods.
- No hidden side effects.
- No magic strings when enums/constants are better.
- No unnecessary abstraction.

---

## 6. Stage-Based Implementation Rule

Build the project stage by stage.

Do not skip stages unless explicitly asked.

Recommended order:

1. Project foundation
2. Database and Flyway migrations
3. Common response and exception handling
4. User entity and repository
5. Authentication and JWT
6. Refresh token with Redis
7. Link CRUD
8. Public redirect
9. Redis redirect cache
10. Kafka click event publishing
11. Analytics consumer and dashboard APIs
12. QR code generation
13. Admin module
14. Rate limiting
15. Testing
16. CI/CD
17. Docker deployment
18. Frontend dashboard

When asked to implement a feature, identify which stage it belongs to and avoid implementing unrelated future features.

---

## 7. Backend Coding Rules

### Controllers

Controllers should:

- Receive requests.
- Validate input using request DTOs.
- Call service methods.
- Return response DTOs.
- Not contain business logic.

### Services

Services should:

- Own business rules.
- Validate ownership.
- Handle transactions.
- Call repositories.
- Publish events when needed.
- Evict cache when data changes.

### Repositories

Repositories should:

- Extend Spring Data JPA repositories.
- Use derived queries for simple cases.
- Use `@Query` only when needed.
- Use `Specification` for dynamic filtering.
- Avoid leaking entities outside service boundaries.

### Mappers

Mappers should:

- Use MapStruct for entity-to-DTO conversion.
- Avoid manual repetitive mapping when MapStruct is suitable.
- Never put business logic inside mappers.

---

## 8. Database and Flyway Rules

Use Flyway for every schema change.

Migration naming:

```text
V1__init_schema.sql
V2__add_refresh_tokens.sql
V3__add_click_events.sql
```

Database rules:

- Use UUID primary keys.
- Use `TIMESTAMPTZ` for timestamps.
- Use foreign keys for required relationships.
- Add indexes for foreign keys and frequent search fields.
- Do not edit old migrations after they have been applied.
- Create a new migration for schema changes.
- Keep database column names in `snake_case`.
- Keep Java fields in `camelCase`.

Important tables:

- `users`
- `refresh_tokens`
- `links`
- `click_events`
- `daily_link_stats`
- `audit_logs`

---

## 9. Entity Rules

Entity rules:

- Prefer UUID IDs.
- Use a shared `BaseEntity` for `createdAt` and `updatedAt`.
- Use JPA auditing for timestamps.
- Keep entity relationships intentional.
- Avoid unnecessary bidirectional relationships.
- Avoid exposing entities directly from controllers.

Recommended base entity fields:

```text
id
createdAt
updatedAt
```

Do not add `createdBy` unless the feature really needs auditing beyond normal ownership fields.

For `links`, `user_id` already represents ownership.

---

## 10. Security Rules

Security is mandatory.

Rules:

- Never store plain-text passwords.
- Use BCrypt for password hashing.
- Use JWT access tokens for stateless authentication.
- Use Redis-backed refresh tokens.
- Validate token expiration.
- Validate token signature.
- Reject disabled or deleted users when needed.
- Use role-based authorization for admin endpoints.
- Use ownership checks for user-owned resources.
- Never allow a user to update, delete, or view another user's private link data.

Common ownership rule:

```text
Current authenticated user ID must match resource owner ID.
```

If not, throw a forbidden/access-denied application exception.

---

## 11. JWT and Current User Rules

The authenticated user should be resolved from the Spring Security context.

Recommended flow:

```text
HTTP request
-> JWT filter
-> parse and validate access token
-> load user details
-> create Authentication
-> store Authentication in SecurityContextHolder
-> controller/service uses CurrentUserService
```

Do not parse JWT manually inside every controller.

Use a dedicated service such as:

```text
CurrentUserService.getCurrentUser()
CurrentUserService.getCurrentUserId()
```

---

## 12. Link Module Rules

The link module is the core MVP feature.

Required behavior:

- Create short link.
- Generate unique short code.
- Support custom alias.
- Validate original URL.
- Validate alias format.
- Prevent duplicate alias.
- Allow title update.
- Allow destination URL update.
- Allow expiration update.
- Allow active/inactive toggle.
- Support pagination and filtering.
- Ensure user ownership.

Short code generation must handle collision safely.

Do not assume one generated code is always unique.

---

## 13. Redirect Rules

Public redirect must be fast and safe.

Redirect behavior:

1. Receive `shortCode`.
2. Check Redis cache.
3. If cache miss, query PostgreSQL.
4. Validate link exists.
5. Validate link is active.
6. Validate link is not expired.
7. Publish click event asynchronously.
8. Return HTTP redirect to original URL.

Do not write click analytics synchronously in the redirect request if Kafka is available.

---

## 14. Redis Rules

Use Redis for:

- Refresh tokens.
- Redirect cache.
- Rate limiting.

Cache key examples:

```text
refresh-token:{userId}:{tokenId}
link:short-code:{shortCode}
rate-limit:login:{ip}
rate-limit:redirect:{ip}
```

When a link changes, evict its redirect cache.

When a user logs out, delete the refresh token from Redis.

---

## 15. Kafka Rules

Use Kafka for click events after redirect is stable.

Topic:

```text
click-events
```

Producer:

- Called by redirect service.
- Publishes click event quickly.
- Must not block the redirect flow for unnecessary work.

Consumer:

- Reads click events.
- Persists raw event data.
- Updates analytics tables if implemented.

Kafka failure should not break basic redirect unless the product explicitly requires guaranteed analytics.

---

## 16. Analytics Rules

Analytics should answer practical questions:

- How many total clicks does a link have?
- How many clicks happened today?
- What is the click trend by day?
- Which referrers are sending traffic?
- Which devices and browsers are used?
- Which countries are visitors from?

Start simple:

1. Store raw click events.
2. Query basic aggregates.
3. Add daily summary table later if needed.

Do not over-optimize analytics before basic data is reliable.

---

## 17. API Response Rules

Use a consistent response format.

Success example:

```json
{
  "success": true,
  "code": "LINK_CREATED",
  "message": "Link created successfully",
  "data": {},
  "timestamp": "2026-06-11T21:00:00+07:00"
}
```

Error example:

```json
{
  "success": false,
  "code": "LINK_NOT_FOUND",
  "message": "Link was not found",
  "errors": [],
  "timestamp": "2026-06-11T21:00:00+07:00"
}
```

Do not return raw exception messages to clients.

---

## 18. Exception Handling Rules

Use a global exception handler.

Required exception categories:

- Validation error
- Unauthorized
- Forbidden
- Not found
- Conflict
- Bad request
- Internal server error

Use project-specific error codes.

Do not throw generic `RuntimeException` for known business errors.

---

## 19. Validation Rules

Validate request DTOs using Bean Validation.

Examples:

- `@NotBlank`
- `@Email`
- `@Size`
- `@Pattern`
- `@Future`

URL validation must reject unsafe protocols.

Allowed URL protocols:

```text
http
https
```

Rejected examples:

```text
javascript:alert(1)
file:///etc/passwd
data:text/html;base64,...
```

---

## 20. Testing Rules

Every important feature should have tests.

Required tests:

- Service unit tests.
- Repository integration tests.
- Authentication integration tests.
- Link CRUD integration tests.
- Redirect integration tests.
- Security ownership tests.

Use Testcontainers when testing PostgreSQL, Redis, or Kafka behavior.

Do not rely only on manual Postman testing.

---

## 21. Documentation Rules

When implementing or changing a major feature, update relevant documentation.

Possible files:

- `README.md`
- `roadmap.md`
- `docs/api.md`
- `docs/database.md`
- `docs/security.md`
- `docs/deployment.md`
- `docs/decisions.md`

Keep documentation clear enough for recruiters and future maintainers.

---

## 22. Git and Commit Rules

Use clear commit messages.

Recommended style:

```text
feat(auth): add JWT login flow
fix(link): prevent duplicate custom alias
refactor(common): simplify API response wrapper
test(security): add ownership access tests
docs(readme): update local setup guide
```

Do not mix unrelated changes in one commit.

---

## 23. Agent Workflow

For every coding task:

1. Read the task carefully.
2. Identify the related roadmap stage.
3. Inspect existing code before editing.
4. Make the smallest correct change.
5. Preserve architecture boundaries.
6. Add or update tests when appropriate.
7. Run relevant tests if possible.
8. Update docs if the behavior changes.
9. Explain what changed clearly.

Do not rewrite large parts of the project unless the user explicitly requests a refactor.

---

## 24. Things Agents Must Not Do

Do not:

- Convert the project to microservices too early.
- Add random dependencies without a clear reason.
- Put business logic in controllers.
- Return entities directly from APIs.
- Ignore ownership checks.
- Store plain-text passwords or tokens.
- Hard-code secrets.
- Break Flyway migration history.
- Use `ddl-auto=create` as a replacement for migrations.
- Implement frontend before backend MVP foundations are stable.
- Add Kafka before the basic redirect flow works.
- Add complex analytics before raw click events are reliable.
- Silently change package names, group ID, Java version, or Spring Boot version.

---

## 25. Definition of Done

A feature is done only when:

- The code compiles.
- The feature follows the roadmap.
- The API contract is clear.
- Validation is implemented.
- Security rules are respected.
- Ownership checks are included where needed.
- Errors use the standard response format.
- Database changes use Flyway.
- Tests are added or updated when appropriate.
- Documentation is updated for important behavior.

---

## 26. Final Project Goal

The final MVP should allow the developer to say in an interview:

> I built a production-style link management and analytics platform using Java 21, Spring Boot, PostgreSQL, Redis, Kafka, Docker, and a modular monolith architecture. The system supports secure authentication, ownership-based authorization, fast Redis-backed redirects, Kafka-based click tracking, analytics dashboards, testing, CI/CD, and deployment-ready infrastructure.

Everything implemented in this repository should support that goal.
