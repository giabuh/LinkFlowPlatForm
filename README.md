# LinkFlowPlatform

> Production-style link management, URL shortening, and click analytics platform built with Java 21, Spring Boot, PostgreSQL, Redis, Kafka, Docker, and a modular monolith architecture.

LinkFlowPlatform is not intended to be a tiny URL shortener demo. The goal is to build a realistic SaaS-style platform that can be deployed for real users and presented confidently in a developer CV or technical interview.

The platform allows users to create, manage, share, and analyze short links with secure authentication, fast redirection, asynchronous click tracking, and analytics dashboards.

---

## 1. Project Purpose

Most beginner portfolio projects only show simple CRUD operations. LinkFlowPlatform is designed to demonstrate production-level backend engineering skills:

- Secure authentication and authorization.
- Clean REST API design.
- PostgreSQL schema design with Flyway migrations.
- Redis caching and refresh token storage.
- Kafka-based asynchronous event processing.
- Analytics-oriented data modeling.
- Modular monolith architecture.
- Docker-based local and production deployment.
- Testing, observability, and CI/CD readiness.

This project is suitable for a CV because it shows practical system design, not only basic CRUD.

---

## 2. Product Vision

LinkFlowPlatform helps users manage links professionally.

A user should be able to:

- Register and log in securely.
- Create short links from long URLs.
- Use custom aliases if available.
- Edit link title, destination URL, expiration date, and active status.
- Delete or disable links.
- Generate QR codes for short links.
- Share short links publicly.
- Redirect visitors quickly.
- Track clicks asynchronously.
- View analytics by link, day, browser, device, referrer, and country.
- Manage account settings.

An admin should be able to:

- View users and links.
- Disable abusive links.
- Monitor suspicious traffic.
- Review system-level analytics.

The system should be deployable as a real application, even if the first version starts as a modular monolith.

---

## 3. MVP Definition

The MVP is intentionally strong. It should be large enough to impress recruiters but still realistic for one developer to complete step by step.

### MVP Core Features

1. Authentication and authorization
   - Register.
   - Login.
   - JWT access token.
   - Redis-backed refresh token.
   - Logout.
   - Role-based authorization.

2. User link management
   - Create link.
   - Generate random short code.
   - Create custom alias.
   - Update link.
   - Soft delete or deactivate link.
   - Search, filter, sort, and paginate links.

3. Public redirect
   - Redirect by short code.
   - Validate link status.
   - Validate expiration.
   - Use Redis cache for fast lookup.
   - Publish click event to Kafka.

4. Click analytics
   - Store click events.
   - Aggregate total clicks.
   - View clicks by date.
   - View referrer, browser, device, and country data.
   - Provide dashboard APIs.

5. QR code
   - Generate QR code for a short link.
   - Download QR code image.

6. Admin module
   - View all users.
   - View all links.
   - Disable suspicious links.
   - View basic system statistics.

7. Production readiness
   - Docker Compose for local infrastructure.
   - Flyway migrations.
   - API validation.
   - Global exception handling.
   - Standard API response format.
   - Unit and integration tests.
   - CI workflow.
   - Basic monitoring and logging.

---

## 4. Tech Stack

### Backend

- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Validation
- Spring Data Redis
- Spring for Apache Kafka
- MapStruct
- Lombok
- Flyway

### Database and Infrastructure

- PostgreSQL
- Redis
- Apache Kafka
- Docker
- Docker Compose

### Testing

- JUnit 5
- Mockito
- Testcontainers
- Spring Boot Test

### Frontend

React + TypeScript SPA with Vite, TanStack Router, TanStack Query, React Hook Form, Zod, Zustand, and Tailwind CSS; use SPA for the user dashboard/admin UI, but keep public short-link redirects handled directly by the backend.

Recommended frontend direction for CV value: React + Vite or Next.js.

---

## 5. Architecture Style

The project follows a modular monolith architecture.

A modular monolith means the application is deployed as one backend service, but the source code is separated by business modules. This gives the project clean boundaries without the complexity of microservices too early.

Recommended modules:

```text
com.giabao.linkflow
├── common
│   ├── dto
│   ├── exception
│   ├── response
│   └── validation
├── config
├── security
├── user
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── mapper
│   ├── repository
│   └── service
├── auth
│   ├── controller
│   ├── dto
│   ├── service
│   └── token
├── link
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── mapper
│   ├── repository
│   ├── service
│   └── specification
├── redirect
│   ├── controller
│   └── service
├── analytics
│   ├── consumer
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
├── qrcode
├── admin
└── audit
```

Rules:

- Keep modules separated by business capability.
- Do not put all business logic into controllers.
- Controllers should validate input and call services.
- Services should own business rules.
- Repositories should only handle persistence.
- Mappers should convert between entities and DTOs.
- Security checks must happen before data mutation.

---

## 6. High-Level System Flow

### Link Creation Flow

```text
Client
  -> POST /api/v1/links
  -> JWT Filter validates access token
  -> LinkController
  -> LinkService
  -> ShortCodeGenerator
  -> LinkRepository
  -> PostgreSQL
  -> LinkResponse
```

### Redirect Flow

```text
Visitor
  -> GET /r/{shortCode}
  -> RedirectController
  -> RedirectService
  -> Redis lookup
  -> PostgreSQL fallback if cache miss
  -> Validate active and expiration status
  -> Publish click event to Kafka
  -> HTTP 302 redirect to original URL
```

### Click Analytics Flow

```text
RedirectService
  -> Kafka topic: click-events
  -> Analytics consumer
  -> Store raw click event
  -> Update analytics tables or query views
  -> Dashboard API reads analytics
```

---

## 7. Database Overview

Recommended starting tables:

- `users`
- `refresh_tokens`
- `links`
- `click_events`
- `daily_link_stats`
- `audit_logs`

Important design decisions:

- Use UUID primary keys.
- Use `TIMESTAMPTZ` for timestamps.
- Use Flyway for every schema change.
- Use unique indexes for email and short code.
- Use indexes for foreign keys and common search filters.
- Prefer soft deletion for user-owned business records.

Example important indexes:

```sql
CREATE INDEX idx_links_user_id ON links(user_id);
CREATE UNIQUE INDEX idx_links_short_code ON links(short_code);
CREATE INDEX idx_links_created_at ON links(created_at);
CREATE INDEX idx_click_events_link_id ON click_events(link_id);
CREATE INDEX idx_click_events_clicked_at ON click_events(clicked_at);
```

---

## 8. API Design Principles

Base path:

```text
/api/v1
```

Example endpoints:

```text
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout

POST   /api/v1/links
GET    /api/v1/links
GET    /api/v1/links/{id}
PATCH  /api/v1/links/{id}
DELETE /api/v1/links/{id}
GET    /api/v1/links/{id}/analytics
GET    /api/v1/links/{id}/qr

GET    /r/{shortCode}

GET    /api/v1/admin/users
GET    /api/v1/admin/links
PATCH  /api/v1/admin/links/{id}/disable
```

Recommended response format:

```json
{
  "success": true,
  "code": "LINK_CREATED",
  "message": "Link created successfully",
  "data": {},
  "timestamp": "2026-06-11T21:00:00+07:00"
}
```

Recommended error format:

```json
{
  "success": false,
  "code": "LINK_NOT_FOUND",
  "message": "Link was not found",
  "errors": [],
  "timestamp": "2026-06-11T21:00:00+07:00"
}
```

---

## 9. Security Principles

Security is a core feature, not an optional layer.

Required rules:

- Passwords must be hashed with BCrypt.
- Never store plain-text passwords.
- Access tokens must be short-lived.
- Refresh tokens should be stored in Redis or a dedicated table.
- Logout must invalidate refresh tokens.
- Users can only access their own links.
- Admin APIs require admin role.
- Public redirect must not expose private user data.
- Validate URLs before storing them.
- Block dangerous protocols such as `javascript:` and `file:`.
- Apply rate limiting to auth and redirect endpoints.

---

## 10. Redis Usage

Redis should be used for practical production-style problems:

1. Refresh token storage
   - Store refresh token ID or token hash.
   - Set TTL based on refresh token expiration.
   - Delete on logout.

2. Redirect cache
   - Cache short code to original URL mapping.
   - Use TTL to prevent stale data.
   - Evict cache when a link is updated, disabled, or deleted.

3. Rate limiting
   - Track request count by IP or user ID.
   - Apply stricter limits to login and redirect endpoints.

---

## 11. Kafka Usage

Kafka should be used after the basic redirect flow is working.

Do not make redirect slow by writing every click synchronously.

Recommended topic:

```text
click-events
```

Click event payload example:

```json
{
  "eventId": "uuid",
  "linkId": "uuid",
  "shortCode": "abc123",
  "clickedAt": "2026-06-11T21:00:00+07:00",
  "ipAddress": "hashed-or-masked-ip",
  "userAgent": "Mozilla/5.0",
  "referrer": "https://example.com"
}
```

Kafka demonstrates:

- Asynchronous processing.
- Event-driven architecture.
- Better redirect performance.
- Scalable analytics pipeline design.

---

## 12. Testing Strategy

Testing is mandatory for CV quality.

Recommended minimum coverage:

- Unit tests for services.
- Unit tests for short code generator.
- Unit tests for JWT/token service.
- Integration tests for repositories.
- Integration tests for authentication flow.
- Integration tests for link CRUD.
- Integration tests for redirect flow.
- Testcontainers for PostgreSQL, Redis, and Kafka when possible.

Important test cases:

- Duplicate email registration fails.
- Login with wrong password fails.
- User cannot access another user's link.
- Expired link cannot redirect.
- Disabled link cannot redirect.
- Custom alias must be unique.
- Redis cache is evicted after link update.
- Click event is published after redirect.

---

## 13. Local Development

### Prerequisites

- Java 21
- Docker
- Docker Compose
- Git
- IntelliJ IDEA or another Java IDE

### Run infrastructure

```bash
docker compose up -d
```

### Run backend

```bash
./gradlew bootRun
```

### Run tests

```bash
./gradlew test
```

### Build project

```bash
./gradlew clean build
```

---

## 14. Environment Variables

Example variables:

```env
SPRING_PROFILES_ACTIVE=local
DB_HOST=localhost
DB_PORT=5432
DB_NAME=linkflow_db
DB_USERNAME=linkflow_user
DB_PASSWORD=linkflow_password
REDIS_HOST=localhost
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
JWT_SECRET=change-this-secret-in-production
JWT_ACCESS_TOKEN_TTL_MINUTES=15
JWT_REFRESH_TOKEN_TTL_DAYS=7
APP_BASE_URL=http://localhost:8080
```

Never commit real production secrets.

---

## 15. Deployment Direction

Recommended deployment stages:

1. Local Docker Compose
2. Single VPS deployment
3. Docker Compose production deployment
4. Optional cloud deployment
5. Optional frontend deployment

Minimum production components:

- Backend container
- PostgreSQL
- Redis
- Kafka or Redpanda
- Reverse proxy such as Nginx or Caddy
- HTTPS certificate
- Environment-based secrets

---

## 16. Documentation Files

Recommended repository documentation:

```text
README.md              # Human-facing project overview
AGENTS.md              # AI agent implementation guide
roadmap.md             # Full product and engineering roadmap
docs/
├── architecture.md     # System architecture and module boundaries
├── api.md              # API contract
├── database.md         # Database schema and migration notes
├── security.md         # Auth, JWT, ownership, and rate limiting
├── deployment.md       # Docker and production deployment guide
└── decisions.md        # Architecture Decision Records
```

---

## 17. CV Positioning

Suggested CV bullet points after MVP completion:

- Built a production-style link management and analytics platform using Java 21, Spring Boot, PostgreSQL, Redis, Kafka, and Docker.
- Designed a modular monolith architecture with secure JWT authentication, role-based access control, and ownership-based authorization.
- Implemented high-performance public redirects using Redis caching and asynchronous Kafka-based click event tracking.
- Designed PostgreSQL schemas and Flyway migrations for users, links, refresh tokens, click events, analytics, and audit logs.
- Added testing, validation, global exception handling, CI workflow, Docker deployment, and observability-ready logging.

---

## 18. Current Development Priority

Follow this order:

1. Project foundation
2. Database and Flyway
3. Common response and exception handling
4. User and auth module
5. Link CRUD module
6. Public redirect module
7. Redis cache
8. Kafka click events
9. Analytics module
10. QR code module
11. Admin module
12. Testing and CI/CD
13. Deployment
14. Frontend dashboard
15. Advanced production features

Do not start Kafka, analytics, or frontend before the core link and auth flows are stable.

---

## 19. Repository Status

This project is under active development.

The main source of truth for implementation order is:

1. `roadmap.md`
2. `AGENTS.md`
3. `README.md`
4. Source code and tests

---

## 20. License

This project can use the MIT License unless a different license is selected later.
