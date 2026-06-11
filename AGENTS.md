# AGENTS.md

## Project Overview

**LinkFlow Platform** is a backend-focused URL shortening and link analytics platform built with **Java 21, Spring Boot, PostgreSQL, Flyway, and JWT Security**.

The platform allows authenticated users to create short links from long URLs, manage their own links, redirect visitors to the original destination, and track basic click analytics.

This project must be developed as a production-style backend portfolio project, not as a simple CRUD demo.

---

## Main Problem

Long URLs are difficult to share, remember, manage, and track.

LinkFlow Platform solves this by providing:

* Short and clean URLs
* User-owned link management
* Fast redirection
* Link expiration
* Active/inactive link control
* Click counting
* Basic click analytics

Example:

```text
Original URL: https://example.com/products/spring-boot-course?ref=facebook
Short URL:    https://linkflow.io/aB92xK
```

---

## Target Users

The system is designed for:

* Marketing teams
* Social media managers
* SaaS platforms
* Businesses
* Internal company teams
* Developers who need trackable links

---

## Core MVP Features

Build the MVP first.

Required features:

1. User registration
2. User login with JWT
3. Password hashing with BCrypt
4. Create short links
5. Generate unique short codes
6. Redirect short links to original URLs
7. List authenticated user's links
8. Update link information
9. Activate and deactivate links
10. Delete links
11. Track total click count
12. Store basic click events
13. View basic analytics for each link
14. Pagination, sorting, and filtering
15. Global exception handling
16. Consistent API response format

---

## Future Features

Do not implement these before the MVP is stable:

* Redis cache for fast redirects
* Async click tracking with Redis Stream or Kafka
* QR code generation
* Custom aliases
* Custom domains
* Admin moderation
* Blocked domains
* Device, browser, and country analytics
* API keys
* CI/CD
* Metrics and logging dashboard

---

## Core Domain Models

### User

A user owns many links.

Required fields:

* id
* email
* passwordHash
* fullName
* role
* createdAt
* updatedAt

### Link

A link belongs to one user.

Required fields:

* id
* userId
* originalUrl
* shortCode
* title
* clickCount
* active
* expiresAt
* createdAt
* updatedAt

### ClickEvent

A click event belongs to one link.

Required fields:

* id
* linkId
* clickedAt
* ipAddress
* userAgent
* referer

---

## Database Direction

Use **Flyway** as the source of truth for database schema.

Do not rely on Hibernate auto-DDL to create or update tables.

Recommended setting:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Recommended tables:

```text
users
links
click_events
```

Recommended indexes:

```sql
CREATE UNIQUE INDEX idx_links_short_code ON links(short_code);
CREATE INDEX idx_links_user_id ON links(user_id);
CREATE INDEX idx_click_events_link_id ON click_events(link_id);
CREATE INDEX idx_click_events_clicked_at ON click_events(clicked_at);
```

---

## Architecture Direction

Use package-by-feature architecture.

Recommended structure:

```text
com.giabao.linkflow
├── auth
├── user
├── link
├── redirect
├── analytics
├── common
├── security
└── config
```

Each feature package may contain:

```text
controller
service
repository
dto
mapper
entity
exception
```

Avoid putting all files into only generic top-level packages such as `controller`, `service`, `repository`, and `entity`.

---

## API Direction

Suggested APIs:

```http
POST   /api/v1/auth/register
POST   /api/v1/auth/login

POST   /api/v1/links
GET    /api/v1/links
GET    /api/v1/links/{id}
PATCH  /api/v1/links/{id}
DELETE /api/v1/links/{id}
PATCH  /api/v1/links/{id}/activate
PATCH  /api/v1/links/{id}/deactivate

GET    /{shortCode}

GET    /api/v1/links/{id}/analytics
GET    /api/v1/links/{id}/click-events
```

The redirect endpoint must be public. User management and analytics endpoints must be protected.

---

## Security Rules

Security is mandatory.

Required rules:

* Use JWT authentication.
* Store user ID in the JWT subject.
* Use BCrypt for password hashing.
* Protect all authenticated user APIs.
* Keep the redirect endpoint public.
* Users can only access their own links.
* Users must not view, update, delete, or analyze another user's links.
* Use role enum values: `USER`, `ADMIN`.
* Store enum values as strings, not ordinals.

---

## Clean Code Rules

The coding agent must follow these rules:

* Keep controllers thin.
* Put business logic in services.
* Use DTOs for requests and responses.
* Never expose JPA entities directly.
* Use constructor injection.
* Validate request DTOs.
* Use `@Transactional` for write operations.
* Use `@Transactional(readOnly = true)` for query operations.
* Avoid duplicated logic.
* Avoid magic strings and magic numbers.
* Use global exception handling.
* Return consistent response shapes.
* Use pagination for list APIs.
* Avoid N+1 queries.
* Keep methods small and intention-revealing.

---

## Redirect Flow

The redirect flow should work like this:

```text
Visitor opens short URL
        ↓
Backend receives shortCode
        ↓
Find link by shortCode
        ↓
Check link exists
        ↓
Check link is active
        ↓
Check link is not expired
        ↓
Increase click count
        ↓
Store click event
        ↓
Redirect to original URL
```

For the MVP, direct PostgreSQL access is acceptable.

For future scaling, Redis can cache:

```text
shortCode -> originalUrl
```

---

## Short Code Generation

Short codes must be unique and URL-safe.

Recommended MVP approach:

1. Generate a random Base62 code.
2. Check uniqueness through the database.
3. Use a unique constraint on `links.short_code`.
4. Retry if a collision happens.
5. Stop after a maximum retry limit.

Allowed characters:

```text
abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789
```

---

## Development Order

Implement the project in this order:

1. Project setup
2. Docker PostgreSQL
3. Flyway migrations
4. User module
5. Authentication and JWT
6. Link module
7. Short code generator
8. Redirect flow
9. Link management APIs
10. Click tracking
11. Basic analytics
12. Pagination and filtering
13. Global exception handling
14. Tests
15. README documentation

Do not add Redis, Kafka, or advanced analytics before the MVP works correctly.

---

## Final Goal

Build LinkFlow Platform as a strong Backend Java Spring Boot CV project.

The project should prove knowledge of:

* REST API design
* Authentication
* Authorization
* Database migration
* Ownership-based security
* Fast redirect logic
* Analytics modeling
* Clean architecture
* Production-ready backend practices
