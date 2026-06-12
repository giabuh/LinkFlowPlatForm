# LinkFlowPlatform Roadmap

> This file is the main engineering roadmap for LinkFlowPlatform.  
> It is written for both human developers and AI coding agents such as Claude, Codex.
> Follow this document when deciding what to build, what to avoid, and how each stage should be completed.

---

## 1. Product Vision

LinkFlowPlatform is a production-style link management, URL shortening, and click analytics platform.

It is not just a simple URL shortener. The goal is to build a real SaaS-style platform that can be shown in a CV, discussed in interviews, and deployed for real users.

The platform allows users to:

- Register and log in securely.
- Create short links from long URLs.
- Use custom aliases when available.
- Manage links from a personal dashboard.
- Enable, disable, update, or delete their own links.
- Set expiration dates for links.
- Generate QR codes for links.
- Redirect visitors quickly from short links to original URLs.
- Track click events.
- View analytics by time, browser, device, country, referrer, and link.
- Use the system safely with rate limiting, validation, abuse protection, and monitoring.

For recruiters, the project should demonstrate:

- Java 21 and Spring Boot backend engineering.
- Clean architecture and modular monolith design.
- Secure authentication and authorization.
- PostgreSQL schema design and Flyway migrations.
- Redis caching and token/session support.
- Kafka-based asynchronous event processing.
- Real analytics pipeline design.
- REST API design.
- Testing strategy.
- Docker deployment.
- CI/CD.
- Observability with logs, metrics, tracing, and health checks.
- Practical product thinking.

---

## 2. Core Principle

Build a modular monolith first.

Do not start with microservices. A modular monolith is easier to develop, easier to test, easier to deploy, and still demonstrates strong system design if the boundaries are clean.

The first production-grade MVP should be one backend application with strong internal modules:

```text
linkflow-platform
├── auth
├── user
├── link
├── redirect
├── analytics
├── event
├── admin
├── security
├── common
└── config
```

Later, if needed, some modules can be extracted into separate services. However, that is not required for the MVP.

---

## 3. Recommended Technology Stack

### 3.1 Backend

Use:

- Java 21
- Spring Boot 3.5.x
- Gradle Kotlin DSL
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Actuator
- MapStruct
- Lombok
- Flyway
- PostgreSQL
- Redis
- Kafka
- Docker and Docker Compose

Do not use Spring Boot 4.x for the first production MVP unless the whole project intentionally upgrades later. Spring Boot 3.5.x is safer for learning, library compatibility, and CV discussion.

### 3.2 Frontend

Recommended:

- React or Next.js
- TypeScript
- Tailwind CSS
- TanStack Query
- React Hook Form
- Zod
- Recharts

Frontend is not mandatory at the very beginning, but the final MVP should have at least a clean dashboard because recruiters and users understand products better when they can see them.

### 3.3 Database

Use PostgreSQL as the main relational database.

Use Flyway for schema versioning.

Never rely on `spring.jpa.hibernate.ddl-auto=update` for production-like development.

Development value:

- PostgreSQL proves real database design ability.
- Flyway proves migration discipline.
- Indexes and constraints prove performance and data integrity thinking.

### 3.4 Cache and Session Support

Use Redis for:

- Redirect cache.
- Refresh token storage.
- Rate limiting counters.
- Temporary verification tokens if email verification is added.

### 3.5 Messaging

Use Kafka for click event processing.

Reason:

- Public redirect should be fast.
- Click tracking should not block user redirection.
- Analytics can be processed asynchronously.
- Kafka demonstrates production-grade event-driven architecture.

### 3.6 Observability

Use:

- Spring Boot Actuator
- Structured logging
- Prometheus metrics
- Grafana dashboards
- OpenTelemetry tracing later

### 3.7 Deployment

Use:

- Dockerfile
- Docker Compose for local production-like environment
- GitHub Actions for CI
- VPS or cloud deployment later
- Nginx reverse proxy
- HTTPS with Let's Encrypt later

---

## 4. MVP Definition

This project has a large MVP, but it must be built gradually.

The MVP is considered complete only when the platform has:

1. User authentication.
2. JWT access tokens.
3. Redis-backed refresh tokens.
4. User-owned link CRUD.
5. Public short link redirect.
6. Custom aliases.
7. Expiring links.
8. Active/inactive links.
9. Soft delete or safe deletion strategy.
10. QR code generation.
11. Basic click tracking.
12. Kafka-based click event ingestion.
13. Analytics dashboard API.
14. Admin user management or admin link moderation.
15. Rate limiting.
16. API validation.
17. Global exception handling.
18. Standard API response format.
19. Flyway migrations.
20. Docker Compose environment.
21. Unit and integration tests.
22. CI pipeline.
23. Deployment documentation.
24. README and architecture docs.
25. CV bullet points and interview explanation.

---

## 5. Non-Negotiable Engineering Rules for Agents

AI agents must follow these rules when modifying the codebase.

### 5.1 Architecture Rules

- Keep the project as a modular monolith.
- Do not mix controller, service, repository, and entity responsibilities.
- Controllers only handle HTTP input/output.
- Services contain business logic.
- Repositories only access data.
- Mappers convert between entities and DTOs.
- Entities should not expose business APIs to controllers directly.
- Never return entities directly from controllers.
- Always return response DTOs.
- Keep package boundaries clean.

### 5.2 Security Rules

- Never store raw passwords.
- Always hash passwords with BCrypt.
- Never expose `passwordHash` in any response.
- Never trust `userId` from request body for ownership-sensitive actions.
- Always get the current user from the security context.
- A user can only manage their own links unless they are an admin.
- Public redirect endpoint must not require authentication.
- Management endpoints must require authentication.
- Admin endpoints must require admin role.
- Refresh tokens must be revocable.
- Logout must invalidate refresh tokens.

### 5.3 Database Rules

- All schema changes must be done through Flyway migrations.
- Do not rely on Hibernate auto-update for schema evolution.
- Add proper indexes for high-read endpoints.
- Add database constraints for uniqueness and foreign keys.
- Prefer UUID primary keys for internal IDs.
- Use stable short codes for public URLs.
- Avoid leaking internal UUIDs in public redirect URLs.

### 5.4 API Rules

- Use RESTful endpoint naming.
- Use clear request and response DTOs.
- Validate input with Bean Validation.
- Use consistent response format.
- Use proper HTTP status codes.
- Use pagination for list endpoints.
- Use filtering and sorting where useful.
- Do not return stack traces to clients.

### 5.5 Code Quality Rules

- Prefer clean, readable code over clever code.
- Keep methods short.
- Use meaningful names.
- Avoid duplicate logic.
- Use MapStruct for mapping when mapping becomes non-trivial.
- Keep exception handling centralized.
- Avoid putting business logic in entities unless it is very small and domain-specific.
- Avoid premature microservices.

### 5.6 Testing Rules

- Add unit tests for business logic.
- Add integration tests for repositories and APIs.
- Test security ownership rules.
- Test redirect behavior.
- Test expired links.
- Test duplicate custom aliases.
- Test refresh token behavior.
- Test Kafka consumer logic when Kafka is introduced.

---

## 6. Target Package Structure

Use this structure as the target direction:

```text
src/main/java/com/giabao/linkflow
├── LinkFlowPlatformApplication.java
│
├── common
│   ├── dto
│   │   ├── ApiResponse.java
│   │   └── PageResponse.java
│   ├── exception
│   │   ├── AppException.java
│   │   └── GlobalExceptionHandler.java
│   ├── response
│   │   └── ErrorCode.java
│   ├── entity
│   │   └── BaseEntity.java
│   └── util
│
├── config
│   ├── JpaAuditingConfig.java
│   ├── SecurityConfig.java
│   ├── RedisConfig.java
│   ├── KafkaConfig.java
│   ├── OpenApiConfig.java
│   └── CorsConfig.java
│
├── security
│   ├── CurrentUserService.java
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   └── SecurityConstants.java
│
├── user
│   ├── entity
│   ├── enums
│   ├── repository
│   ├── service
│   ├── mapper
│   └── dto
│
├── auth
│   ├── controller
│   ├── service
│   ├── dto
│   └── token
│
├── link
│   ├── controller
│   ├── entity
│   ├── repository
│   ├── service
│   ├── mapper
│   ├── dto
│   ├── enums
│   └── specification
│
├── redirect
│   ├── controller
│   └── service
│
├── analytics
│   ├── controller
│   ├── entity
│   ├── repository
│   ├── service
│   ├── dto
│   └── projection
│
├── event
│   ├── producer
│   ├── consumer
│   └── dto
│
└── admin
    ├── controller
    ├── service
    └── dto
```

---

## 7. Database Roadmap

### 7.1 Initial Tables

Start with these core tables:

```text
users
links
refresh_tokens
click_events
```

Later add:

```text
link_daily_stats
audit_logs
api_rate_limits or redis-only rate limit keys
user_settings
teams
team_members
```

### 7.2 users Table

Purpose:

Store account information.

Fields:

```text
id UUID primary key
email varchar unique not null
password_hash varchar not null
full_name varchar
role varchar not null default 'USER'
status varchar not null default 'ACTIVE'
created_at timestamptz not null
updated_at timestamptz not null
```

Rules:

- `email` must be unique.
- `password_hash` must never be exposed.
- `role` should map to a Java enum.
- Start with one role per user: `USER`, `ADMIN`.
- Multiple roles are not needed for MVP unless a real requirement appears.

### 7.3 links Table

Purpose:

Store user-created links.

Fields:

```text
id UUID primary key
user_id UUID not null foreign key
original_url text not null
short_code varchar(32) unique not null
title varchar(255)
description text
active boolean not null default true
expires_at timestamptz null
click_count bigint not null default 0
last_clicked_at timestamptz null
created_at timestamptz not null
updated_at timestamptz not null
deleted_at timestamptz null
```

Rules:

- `short_code` must be unique.
- `original_url` must be validated.
- `user_id` must be required.
- `click_count` can be eventually consistent after Kafka is added.
- `deleted_at` enables soft delete if chosen.

Indexes:

```text
idx_links_user_id
idx_links_short_code
idx_links_created_at
idx_links_user_created_at
idx_links_active
```

### 7.4 refresh_tokens Table or Redis-only Strategy

Preferred MVP strategy:

Use Redis for refresh tokens first.

Redis key pattern:

```text
refresh_token:{userId}:{tokenId}
```

Value:

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "createdAt": "timestamp",
  "expiresAt": "timestamp",
  "revoked": false
}
```

Later, if persistent audit is needed, add a `refresh_tokens` table.

### 7.5 click_events Table

Purpose:

Store detailed click events for analytics.

Fields:

```text
id UUID primary key
link_id UUID not null
short_code varchar(32) not null
clicked_at timestamptz not null
ip_hash varchar(128)
user_agent text
referer text
browser varchar(100)
os varchar(100)
device_type varchar(50)
country varchar(100)
city varchar(100)
created_at timestamptz not null
```

Rules:

- Do not store raw IP unless there is a clear reason.
- Prefer hashing IP for privacy.
- Keep analytics useful without over-collecting personal data.

### 7.6 link_daily_stats Table

Purpose:

Store aggregated analytics for faster dashboards.

Fields:

```text
id UUID primary key
link_id UUID not null
stat_date date not null
total_clicks bigint not null
unique_clicks bigint not null
created_at timestamptz not null
updated_at timestamptz not null
```

Unique constraint:

```text
unique(link_id, stat_date)
```

This table is optional for early MVP but valuable for performance and CV discussion.

---

## 8. API Response Standard

Use one consistent response shape.

Example success response:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request completed successfully",
  "data": {}
}
```

Example error response:

```json
{
  "success": false,
  "code": "LINK_NOT_FOUND",
  "message": "The requested link does not exist",
  "data": null
}
```

Example paginated response:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request completed successfully",
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "last": false
  }
}
```

---

## 9. Error Code Direction

Create clear error codes:

```text
UNCATEGORIZED_ERROR
VALIDATION_ERROR
UNAUTHORIZED
FORBIDDEN
USER_NOT_FOUND
EMAIL_ALREADY_EXISTS
INVALID_CREDENTIALS
TOKEN_EXPIRED
TOKEN_INVALID
REFRESH_TOKEN_INVALID
REFRESH_TOKEN_EXPIRED
LINK_NOT_FOUND
SHORT_CODE_ALREADY_EXISTS
LINK_EXPIRED
LINK_DISABLED
LINK_ACCESS_DENIED
RATE_LIMIT_EXCEEDED
KAFKA_PUBLISH_FAILED
```

Each error code should map to:

- HTTP status
- Stable application code
- User-readable message

---

## 10. Stage 0 — Repository Foundation

### Goal

Create a clean repository foundation so future agents and developers know where everything belongs.

### Tasks

- Create `README.md`.
- Create `roadmap.md`.
- Create `AGENTS.md`.
- Create `docs/architecture.md`.
- Create `docs/api.md`.
- Create `docs/database.md`.
- Create `docs/security.md`.
- Create `docs/deployment.md`.
- Create `.env.example`.
- Create `.gitignore`.
- Create Docker Compose for PostgreSQL only at first.
- Configure Gradle Kotlin DSL.
- Add base Spring Boot dependencies.

### Acceptance Criteria

- Project builds successfully.
- PostgreSQL starts with Docker Compose.
- Application can connect to database.
- Flyway can run first migration.
- README explains how to run locally.

### Agent Notes

- Do not build features before the foundation is clean.
- Do not hard-code secrets.
- Keep environment variables in `.env.example` as examples only.

---

## 11. Stage 1 — Database and Base Entity

### Goal

Create the database schema and shared entity foundation.

### Tasks

- Enable Flyway.
- Create migration `V1__init_schema.sql`.
- Enable PostgreSQL `pgcrypto` extension if UUID is generated in DB.
- Create `users` table.
- Create `links` table.
- Add foreign key from `links.user_id` to `users.id`.
- Add indexes.
- Create `BaseEntity` with:
  - `createdAt`
  - `updatedAt`
- Enable JPA auditing.
- Create `Role` enum.
- Create `UserStatus` enum.
- Create `User` entity.
- Create `Link` entity.

### Acceptance Criteria

- Migrations run from empty database.
- Entities match database columns.
- Application starts without schema mismatch.
- `createdAt` and `updatedAt` work automatically.

### Important Design Decision

For MVP, `Link` already belongs to `User` through `user_id`. You do not need `createdBy` yet unless you want a generic audit system across many entity types.

Use `createdBy` later only if:

- You add admin actions.
- You need audit logs.
- You have many entities requiring creator tracking.

---

## 12. Stage 2 — Common Backend Foundation

### Goal

Build reusable backend infrastructure before business features become large.

### Tasks

- Create `ApiResponse<T>`.
- Create `PageResponse<T>`.
- Create `AppException`.
- Create `ErrorCode` enum.
- Create `GlobalExceptionHandler`.
- Handle validation errors cleanly.
- Configure CORS.
- Configure OpenAPI/Swagger if desired.
- Configure application profiles:
  - `local`
  - `dev`
  - `prod`
  - `test`

### Acceptance Criteria

- All API responses have the same format.
- Validation errors return useful messages.
- Unknown errors do not leak stack traces.
- Profiles are separated cleanly.

### Agent Notes

- Do not duplicate response formatting in every controller.
- Do not return raw exceptions.
- Keep error messages stable.

---

## 13. Stage 3 — Authentication and User Module

### Goal

Implement secure account registration and login.

### Features

- Register
- Login
- Refresh access token
- Logout
- Get current profile
- Update profile
- Change password later

### API Endpoints

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/users/me
PATCH /api/v1/users/me
```

### Tasks

- Create register request DTO.
- Create login request DTO.
- Create auth response DTO.
- Implement password hashing with BCrypt.
- Implement `CustomUserDetails`.
- Implement `CustomUserDetailsService`.
- Implement `JwtService`.
- Implement `JwtAuthenticationFilter`.
- Configure Spring Security.
- Implement `CurrentUserService`.
- Store refresh tokens in Redis.
- Implement logout by deleting refresh token from Redis.

### Access Token Rules

- Short lifetime.
- Contains subject as user ID or email depending on final design.
- Should include role claim if needed.
- Must be verified on protected requests.

Recommended claim design:

```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "role": "USER",
  "type": "ACCESS"
}
```

### Refresh Token Rules

- Long lifetime.
- Stored in Redis.
- Can be revoked.
- Should not be accepted if missing from Redis.

### Acceptance Criteria

- User can register.
- Duplicate email is rejected.
- User can login.
- Login returns access token and refresh token.
- Protected endpoint requires access token.
- Invalid token returns 401.
- User can refresh token.
- Logout invalidates refresh token.

### Agent Notes

- Do not put authentication logic in controllers.
- Do not expose password hash.
- Do not trust user ID from client request body.

---

## 14. Stage 4 — Link Management Core

### Goal

Allow authenticated users to manage their own short links.

### Features

- Create link.
- Create link with generated short code.
- Create link with custom alias.
- List my links.
- Search and filter my links.
- Update title, destination URL, active status, expiration.
- Delete or soft delete link.
- Get link detail.

### API Endpoints

```text
POST   /api/v1/links
GET    /api/v1/links
GET    /api/v1/links/{id}
PATCH  /api/v1/links/{id}
DELETE /api/v1/links/{id}
```

### Request Fields

Create link request:

```json
{
  "originalUrl": "https://example.com/very/long/url",
  "customAlias": "my-link",
  "title": "Example Link",
  "description": "Optional description",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

### Filtering

Support filters:

```text
keyword
active
createdFrom
createdTo
expiresFrom
expiresTo
sort
page
size
```

### Tasks

- Create `CreateLinkRequest`.
- Create `UpdateLinkRequest`.
- Create `LinkFilterRequest`.
- Create `LinkResponse`.
- Create `LinkMapper`.
- Create `LinkRepository`.
- Create `LinkService` interface.
- Create `LinkServiceImpl`.
- Create `ShortCodeGenerator`.
- Create `LinkSpecifications`.
- Implement ownership check.
- Implement duplicate alias check.
- Implement URL validation.
- Implement pagination.

### Short Code Rules

Generated short codes should:

- Be URL-safe.
- Be short enough for usability.
- Avoid confusing characters if possible.
- Be checked for uniqueness.
- Retry on collision.

Example alphabet:

```text
0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ
```

### Acceptance Criteria

- Authenticated user can create a link.
- Custom alias works if available.
- Duplicate custom alias is rejected.
- User can list only their own links.
- User cannot access another user's link management endpoint.
- Expiration date can be set.
- Inactive links are stored but not publicly redirectable.

### Agent Notes

- Ownership check is mandatory.
- Do not use raw SQL unless needed.
- Do not return deleted links unless explicitly requested.

---

## 15. Stage 5 — Public Redirect Module

### Goal

Make short links redirect visitors quickly and correctly.

### Public Endpoint

```text
GET /{shortCode}
```

or, if API and app domain are separate:

```text
GET /r/{shortCode}
```

### Redirect Rules

When a visitor opens a short link:

1. Find link by short code.
2. If not found, return 404 page or API error.
3. If inactive, return disabled link page or API error.
4. If expired, return expired link page or API error.
5. Track click event.
6. Redirect to original URL.

### Tasks

- Create `RedirectController`.
- Create `RedirectService`.
- Implement lookup by short code.
- Implement active check.
- Implement expiration check.
- Return HTTP 302 or 307 redirect.
- Capture request metadata:
  - user agent
  - referer
  - IP address or hashed IP
  - timestamp

### Acceptance Criteria

- Valid short code redirects correctly.
- Unknown short code returns not found.
- Disabled link does not redirect.
- Expired link does not redirect.
- Click count increases or event is recorded.

### Agent Notes

- Redirect endpoint is performance-sensitive.
- Avoid heavy synchronous work in redirect flow.
- Do not block redirect because analytics failed.

---

## 16. Stage 6 — Redis Cache for Redirects

### Goal

Make public redirects fast and reduce database load.

### Cache Strategy

Use Redis key:

```text
link:short_code:{shortCode}
```

Value should contain:

```json
{
  "linkId": "uuid",
  "shortCode": "abc123",
  "originalUrl": "https://example.com",
  "active": true,
  "expiresAt": null
}
```

### Tasks

- Add Redis dependency.
- Configure Redis connection.
- Create cache DTO.
- Cache link lookup by short code.
- Invalidate cache when link is updated, disabled, deleted, or expiration changes.
- Add cache TTL.

### Acceptance Criteria

- First redirect can hit database.
- Repeated redirects hit Redis.
- Updating a link invalidates old cache.
- Disabled or deleted links do not stay redirectable because of stale cache.

### Agent Notes

- Cache invalidation is mandatory.
- Do not cache invalid links for too long.
- Negative caching can be added later for unknown short codes.

---

## 17. Stage 7 — Basic Click Tracking

### Goal

Record click events before adding Kafka.

### Tasks

- Create `ClickEvent` entity.
- Create `ClickEventRepository`.
- Create `ClickTrackingService`.
- Save click event asynchronously if possible.
- Increment link click count.
- Add basic analytics endpoints.

### API Endpoints

```text
GET /api/v1/links/{id}/analytics/summary
GET /api/v1/links/{id}/analytics/clicks
```

### Analytics Summary Response

```json
{
  "totalClicks": 123,
  "uniqueClicks": 80,
  "lastClickedAt": "2026-06-11T10:00:00Z"
}
```

### Acceptance Criteria

- Each redirect produces a click event.
- Link click count updates.
- User can see summary for their own link.
- User cannot see analytics for another user's link.

### Agent Notes

- This stage can be simple.
- Kafka will improve it later.
- Keep the service boundary clean so Kafka can be added without rewriting everything.

---

## 18. Stage 8 — Kafka Event Pipeline

### Goal

Move click tracking out of the redirect request path.

### Architecture

```text
Redirect Request
    ↓
RedirectService
    ↓
Kafka Producer publishes ClickEventMessage
    ↓
Visitor is redirected quickly
    ↓
Kafka Consumer receives event
    ↓
Save click_events
    ↓
Update link click_count and analytics tables
```

### Kafka Topic

```text
link.clicked.v1
```

### Event Payload

```json
{
  "eventId": "uuid",
  "linkId": "uuid",
  "shortCode": "abc123",
  "clickedAt": "2026-06-11T10:00:00Z",
  "ipHash": "hashed-value",
  "userAgent": "Mozilla/5.0",
  "referer": "https://google.com",
  "browser": "Chrome",
  "os": "Ubuntu",
  "deviceType": "Desktop",
  "country": "Vietnam"
}
```

### Tasks

- Add Kafka dependency.
- Add Kafka to Docker Compose.
- Create producer config.
- Create consumer config.
- Create `ClickEventMessage` DTO.
- Create `ClickEventProducer`.
- Create `ClickEventConsumer`.
- Publish event during redirect.
- Consume event and persist it.
- Add error handling for failed events.
- Add idempotency by `eventId` if needed.

### Acceptance Criteria

- Redirect does not wait for database analytics write.
- Kafka topic receives click messages.
- Consumer saves click events.
- Consumer updates click count.
- System still redirects even if Kafka publish fails, depending on chosen fallback.

### Agent Notes

- Keep Kafka event versioned with `.v1` topic naming.
- Do not send entity objects directly as messages.
- Use DTO/event message classes.
- Think about duplicate events. Consumers should be safe enough for MVP.

---

## 19. Stage 9 — Analytics Dashboard API

### Goal

Provide useful analytics that make the project feel like a real SaaS platform.

### Features

- Total clicks.
- Unique clicks estimate.
- Click trend by day.
- Top links.
- Recent clicks.
- Referrer breakdown.
- Browser breakdown.
- Device breakdown.
- Country breakdown.

### API Endpoints

```text
GET /api/v1/analytics/overview
GET /api/v1/links/{id}/analytics/summary
GET /api/v1/links/{id}/analytics/timeseries
GET /api/v1/links/{id}/analytics/referrers
GET /api/v1/links/{id}/analytics/devices
GET /api/v1/links/{id}/analytics/browsers
GET /api/v1/links/{id}/analytics/countries
GET /api/v1/links/{id}/analytics/recent-clicks
```

### Tasks

- Create projection interfaces for aggregate queries.
- Add repository queries for analytics.
- Add service methods.
- Add response DTOs.
- Add date range filtering.
- Add ownership checks.
- Add indexes if queries are slow.

### Acceptance Criteria

- User can view analytics only for their own links.
- Overview dashboard returns useful numbers.
- Link detail analytics returns time series data.
- Queries are paginated where needed.
- Analytics endpoints are not extremely slow on realistic seed data.

### Agent Notes

- Use database aggregation first.
- Add aggregated stats table only when needed.
- Avoid loading thousands of rows into Java just to count them.

---

## 20. Stage 10 — QR Code Feature

### Goal

Allow users to generate QR codes for short links.

### Features

- Generate QR code image for a link.
- Download QR code.
- Show QR code in dashboard.

### API Endpoints

```text
GET /api/v1/links/{id}/qr
```

### Tasks

- Add QR generation library.
- Generate QR from public short URL.
- Return PNG image or base64 depending on frontend needs.
- Ensure ownership check.

### Acceptance Criteria

- User can generate QR code for their own link.
- QR code points to the short URL, not the original URL.
- QR code scan redirects correctly.

### Agent Notes

- Do not store QR images in database for MVP.
- Generate on demand unless performance becomes an issue.

---

## 21. Stage 11 — Admin Module

### Goal

Add admin capabilities to make the platform look production-ready.

### Features

- View users.
- View links across users.
- Disable abusive links.
- View system statistics.
- View recent click volume.

### API Endpoints

```text
GET   /api/v1/admin/users
GET   /api/v1/admin/users/{id}
PATCH /api/v1/admin/users/{id}/status
GET   /api/v1/admin/links
PATCH /api/v1/admin/links/{id}/disable
GET   /api/v1/admin/stats
```

### Tasks

- Add admin authorization.
- Add admin service.
- Add admin DTOs.
- Add filtering and pagination.
- Add audit logs for admin actions later.

### Acceptance Criteria

- Only admin can call admin endpoints.
- Admin can disable suspicious links.
- Normal users cannot access admin APIs.

### Agent Notes

- Role check must be enforced through Spring Security.
- Do not rely only on frontend hiding buttons.

---

## 22. Stage 12 — Rate Limiting and Abuse Protection

### Goal

Protect public and auth endpoints.

### Features

- Rate limit login attempts.
- Rate limit registration attempts.
- Rate limit link creation.
- Rate limit public redirect if needed.
- Block obviously invalid URLs.
- Prevent dangerous protocols.

### URL Validation Rules

Allow:

```text
http://
https://
```

Reject:

```text
javascript:
file:
data:
ftp: optional depending on product decision
localhost/internal IPs: reject in production if SSRF risk matters
```

### Redis Key Examples

```text
rate_limit:login:{ipHash}
rate_limit:create_link:{userId}
rate_limit:redirect:{ipHash}:{shortCode}
```

### Acceptance Criteria

- Too many login attempts return 429.
- Too many link creation attempts return 429.
- Invalid protocols are rejected.
- Rate limit configuration can be changed by environment.

### Agent Notes

- Rate limiting should not make local development painful.
- Use environment-based configuration.

---

## 23. Stage 13 — Frontend MVP

### Goal

Create a usable web interface so the project is demonstrable.

### Pages

```text
/login
/register
/dashboard
/links
/links/new
/links/:id
/settings
/admin/users
/admin/links
```

### Dashboard Features

- Total links.
- Total clicks.
- Top performing links.
- Recent clicks.
- Click trend chart.

### Link Management Features

- Create link form.
- Edit link form.
- Copy short URL.
- Toggle active/inactive.
- Delete link.
- Show QR code.
- Show analytics.

### Technical Rules

- Use TypeScript.
- Use API client abstraction.
- Store access token safely according to chosen strategy.
- Handle refresh token flow carefully.
- Show validation errors clearly.
- Do not hard-code API base URL.

### Acceptance Criteria

- User can register and log in from UI.
- User can create and manage links.
- User can view analytics charts.
- UI is clean enough for demo.
- App works against deployed backend.

### Agent Notes

- Do not over-design UI before backend is stable.
- Make the UI simple, clean, and usable.

---

## 24. Stage 14 — Testing Strategy

### Goal

Prove that the system is reliable.

### Test Types

- Unit tests.
- Repository tests.
- Service tests.
- Controller/API tests.
- Security tests.
- Integration tests with Testcontainers if possible.

### Must-Test Scenarios

Authentication:

- Register success.
- Duplicate email rejected.
- Login success.
- Login invalid password.
- Refresh token success.
- Refresh token revoked.
- Logout invalidates refresh token.

Links:

- Create generated short code.
- Create custom alias.
- Duplicate alias rejected.
- List only current user's links.
- Update own link.
- Cannot update another user's link.
- Delete own link.
- Cannot delete another user's link.

Redirect:

- Valid short code redirects.
- Unknown short code returns 404.
- Expired link does not redirect.
- Disabled link does not redirect.

Analytics:

- Click event saved.
- Click count updated.
- User cannot view another user's analytics.

Kafka:

- Producer publishes click event.
- Consumer persists click event.
- Duplicate event does not break system.

### Acceptance Criteria

- Core service tests pass.
- Security ownership tests pass.
- CI runs tests automatically.

### Agent Notes

- Security tests are not optional.
- Ownership bugs are serious bugs.

---

## 25. Stage 15 — Observability

### Goal

Make the system observable like a production service.

### Features

- Health endpoint.
- Readiness/liveness checks.
- Structured logs.
- Request logging.
- Metrics for redirects.
- Metrics for link creation.
- Metrics for Kafka consumer lag later.
- Tracing later.

### Endpoints

```text
/actuator/health
/actuator/info
/actuator/metrics
/actuator/prometheus
```

### Metrics Ideas

```text
linkflow_redirect_total
linkflow_redirect_failed_total
linkflow_link_created_total
linkflow_auth_login_failed_total
linkflow_click_event_consumed_total
```

### Acceptance Criteria

- Actuator health works.
- Prometheus can scrape metrics.
- Logs contain request ID or correlation ID later.
- Production errors are debuggable.

### Agent Notes

- Do not log passwords or tokens.
- Do not log full Authorization header.
- Avoid logging sensitive user data.

---

## 26. Stage 16 — Docker and Local Infrastructure

### Goal

Make the whole project easy to run locally.

### Services

Docker Compose should eventually include:

```text
postgres
redis
kafka
zookeeper or kraft kafka mode depending on image
backend
frontend
prometheus
grafana
nginx optional
```

### Development Commands

Recommended Makefile commands:

```text
make up
make down
make logs
make restart
make db-reset
make test
make build
```

### Acceptance Criteria

- New developer can run the project from README.
- Local environment does not require manually installing PostgreSQL, Redis, or Kafka.
- Services have health checks where useful.

### Agent Notes

- Keep Docker Compose readable.
- Do not put production secrets in Compose.
- Use `.env` for local only and `.env.example` for documentation.

---

## 27. Stage 17 — CI/CD

### Goal

Show professional software delivery workflow.

### GitHub Actions Pipeline

Minimum pipeline:

```text
on pull request or push:
  - checkout
  - setup Java 21
  - cache Gradle
  - run tests
  - build backend
  - build Docker image later
```

Later pipeline:

```text
- run backend tests
- run frontend tests
- build backend image
- build frontend image
- push images to registry
- deploy to VPS or cloud
```

### Acceptance Criteria

- CI runs on every push or pull request.
- Broken tests block merge.
- Docker image can be built automatically later.

### Agent Notes

- Keep CI simple first.
- Do not add complex deployment before tests are stable.

---

## 28. Stage 18 — Deployment MVP

### Goal

Deploy the platform for real users.

### Deployment Target

Recommended first deployment:

- VPS
- Docker Compose
- PostgreSQL volume
- Redis container
- Kafka container if server has enough resources
- Nginx reverse proxy
- HTTPS with Let's Encrypt

### Production Checklist

- Use strong database password.
- Use environment variables for secrets.
- Use HTTPS.
- Configure CORS for real frontend domain.
- Disable Swagger in production or protect it.
- Configure log rotation.
- Configure backups.
- Configure health checks.
- Configure restart policies.

### Acceptance Criteria

- Backend is reachable from public domain.
- Frontend can call backend.
- Short links redirect publicly.
- Data persists after container restart.
- Basic monitoring exists.

### Agent Notes

- Do not expose PostgreSQL publicly.
- Do not expose Redis publicly.
- Do not expose Kafka publicly.
- Only expose Nginx/backend/frontend as needed.

---

## 29. Stage 19 — Documentation for Recruiters and Future Agents

### Goal

Make the project understandable without reading all code.

### Required Docs

```text
README.md
roadmap.md
AGENTS.md
docs/architecture.md
docs/api.md
docs/database.md
docs/security.md
docs/deployment.md
docs/decisions/ADR-001-modular-monolith.md
docs/decisions/ADR-002-redis-refresh-token.md
docs/decisions/ADR-003-kafka-click-events.md
```

### README Must Include

- Product description.
- Main features.
- Tech stack.
- Architecture diagram.
- Local setup.
- API docs link.
- Screenshots later.
- Deployment link later.

### Architecture Doc Must Include

- Module boundaries.
- Request flow.
- Auth flow.
- Redirect flow.
- Analytics flow.
- Kafka flow.
- Deployment diagram.

### Acceptance Criteria

- A recruiter can understand the value in 2 minutes.
- A developer can run the project in 15 minutes.
- An AI agent can follow the project direction without guessing.

---

## 30. Stage 20 — CV Positioning

### Goal

Translate technical work into strong CV value.

### Possible CV Description

```text
Built LinkFlowPlatform, a production-style URL shortening and analytics SaaS using Java 21, Spring Boot, PostgreSQL, Redis, Kafka, Docker, and React. Implemented JWT authentication, Redis-backed refresh tokens, secure link ownership, high-speed redirect caching, asynchronous click tracking with Kafka, analytics dashboards, Flyway migrations, CI/CD, and Docker-based deployment.
```

### Strong Interview Talking Points

Be ready to explain:

- Why modular monolith instead of microservices.
- Why Redis is used for refresh tokens and redirect caching.
- Why Kafka is used for click events.
- How public redirect stays fast.
- How ownership authorization works.
- How Flyway keeps database changes safe.
- How indexes improve query performance.
- How analytics are aggregated.
- How rate limiting protects the system.
- How the system can scale later.

---

## 31. Suggested Build Order

Follow this order strictly unless there is a strong reason to change it.

```text
1. Repository foundation
2. Docker PostgreSQL
3. Flyway schema
4. BaseEntity and auditing
5. Common response and exception system
6. User entity and repository
7. Auth register/login
8. JWT security
9. Redis refresh token
10. Link CRUD
11. Ownership checks
12. Public redirect
13. Basic click tracking
14. Redis redirect cache
15. Analytics API
16. Kafka click events
17. QR code
18. Admin module
19. Rate limiting
20. Frontend dashboard
21. Tests
22. Observability
23. CI/CD
24. Deployment
25. Documentation and CV polish
```

---

## 32. MVP Completion Checklist

### Backend

- [ ] User registration
- [ ] User login
- [ ] JWT access token
- [ ] Redis refresh token
- [ ] Logout
- [ ] Current user endpoint
- [ ] Link creation
- [ ] Custom alias
- [ ] Link update
- [ ] Link delete
- [ ] Link search/filter/page
- [ ] Public redirect
- [ ] Expired link handling
- [ ] Disabled link handling
- [ ] Click tracking
- [ ] Kafka click events
- [ ] Analytics APIs
- [ ] QR code generation
- [ ] Admin APIs
- [ ] Rate limiting
- [ ] Global exception handling
- [ ] Validation
- [ ] Swagger/OpenAPI

### Database

- [ ] Flyway enabled
- [ ] users table
- [ ] links table
- [ ] click_events table
- [ ] indexes
- [ ] constraints
- [ ] seed admin user strategy

### Infrastructure

- [ ] Dockerfile
- [ ] Docker Compose PostgreSQL
- [ ] Docker Compose Redis
- [ ] Docker Compose Kafka
- [ ] Health checks
- [ ] GitHub Actions CI
- [ ] Deployment guide

### Testing

- [ ] Auth tests
- [ ] Link service tests
- [ ] Ownership tests
- [ ] Redirect tests
- [ ] Analytics tests
- [ ] Kafka tests
- [ ] Repository tests

### Frontend

- [ ] Login page
- [ ] Register page
- [ ] Dashboard page
- [ ] Link list page
- [ ] Link create page
- [ ] Link detail page
- [ ] Analytics charts
- [ ] QR code display
- [ ] Admin page

### Documentation

- [ ] README
- [ ] roadmap.md
- [ ] AGENTS.md
- [ ] architecture.md
- [ ] api.md
- [ ] database.md
- [ ] security.md
- [ ] deployment.md
- [ ] CV bullets

---

## 33. Post-MVP Advanced Features

Build these only after the MVP is stable.

### Team Workspace

- Users can create teams.
- Team members can share links.
- Roles: owner, member, viewer.

### Branded Domains

- Users can use their own domains.
- Example: `go.company.com/product`.

### UTM Builder

- Add campaign parameters.
- Save UTM presets.

### A/B Links

- One short link redirects to multiple destinations by weight.
- Useful for marketing experiments.

### Link Password Protection

- Visitors must enter password before redirect.

### Geo-Based Redirect

- Redirect users to different URLs by country.

### Device-Based Redirect

- Redirect mobile and desktop users differently.

### Webhooks

- Notify external systems when links are clicked.

### Billing Simulation

- Free plan, Pro plan, Admin plan.
- Limit number of links per plan.

### Background Jobs

- Archive old click events.
- Recalculate daily stats.
- Send weekly analytics reports.

---

## 34. Common Mistakes to Avoid

Do not:

- Build microservices too early.
- Skip tests.
- Skip Flyway.
- Return entities from controllers.
- Store raw passwords.
- Store raw refresh tokens without a revocation strategy.
- Let users access other users' links.
- Make redirect slow by doing too much work synchronously.
- Add Kafka before basic click tracking works.
- Add frontend complexity before backend APIs stabilize.
- Put secrets in Git.
- Expose database ports in production.
- Add features without documentation.

---

## 35. Definition of Done for Every Feature

A feature is done only when:

- Database migration exists if needed.
- Entity/repository/service/controller are implemented cleanly.
- Request DTOs and response DTOs are used.
- Validation is added.
- Authorization is checked.
- Errors are handled through `AppException` and `ErrorCode`.
- Tests are added for important behavior.
- API is documented.
- README or docs are updated if behavior changes.

---

## 36. Agent Execution Policy

When an AI coding agent works on this project, it should:

1. Read `README.md`.
2. Read `roadmap.md`.
3. Read `AGENTS.md`.
4. Identify the current stage.
5. Modify only files related to the current task.
6. Avoid large unrelated refactors.
7. Keep naming consistent with existing code.
8. Add tests when implementing business logic.
9. Run or explain relevant checks.
10. Update docs when adding major features.

The agent should not:

- Invent a new architecture without updating this roadmap.
- Replace the chosen stack without a clear reason.
- Add unnecessary libraries.
- Hide breaking changes.
- Leave TODO comments for core logic.

---

## 37. Final Target Architecture

```text
Client / Browser
    ↓
Frontend Dashboard
    ↓
Backend Spring Boot API
    ├── Auth Module
    ├── User Module
    ├── Link Module
    ├── Redirect Module
    ├── Analytics Module
    ├── Admin Module
    └── Event Module
          ↓
        Kafka
          ↓
   Click Event Consumer
          ↓
PostgreSQL ← Redis Cache
    ↓
Prometheus / Grafana / Logs
```

---

## 38. Final Product Standard

The final MVP should feel like a real product, not a school assignment.

A user should be able to:

1. Open the website.
2. Register an account.
3. Log in.
4. Create a short link.
5. Share the short link.
6. See the redirect work.
7. See click analytics.
8. Generate a QR code.
9. Manage links safely.

A recruiter should be able to see:

1. Clean code.
2. Real architecture.
3. Real database design.
4. Real security.
5. Real async processing.
6. Real deployment thinking.
7. Clear documentation.
8. Strong engineering decisions.

---

## 39. Current Priority

If the project is still early, focus on this immediate sequence:

```text
1. Stabilize Gradle dependencies.
2. Stabilize Docker PostgreSQL.
3. Finalize Flyway V1 schema.
4. Implement BaseEntity and auditing.
5. Implement common response and exception format.
6. Implement User entity and repository.
7. Implement Auth register/login.
8. Implement JWT security.
9. Implement CurrentUserService.
10. Implement Link CRUD with ownership checks.
```

Do not jump to Kafka, frontend, or analytics before this foundation is stable.

---

## 40. Roadmap Maintenance Rule

Whenever the project direction changes, update this file.

This roadmap is the source of truth for:

- Feature order.
- Architecture direction.
- Technology choices.
- MVP scope.
- Agent behavior.
- CV positioning.

