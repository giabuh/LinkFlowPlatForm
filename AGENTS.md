# AGENTS.md

## Project Name

**LinkFlow Platform**

## Project Overview

LinkFlow Platform is a production-style URL shortening and link analytics platform built with **Java 21**, **Spring Boot**, **PostgreSQL**, and **Flyway**.

The system allows users to create short, clean, shareable links from long URLs. When someone visits a short link, the backend redirects the request to the original URL and records click analytics.

This project is not intended to be a basic CRUD application. It should be designed as a real backend portfolio project that demonstrates security, scalability, clean architecture, database design, caching readiness, and production-level API practices.

---

## Main Problem This Project Solves

Long URLs are difficult to share, remember, track, and manage.

LinkFlow Platform solves this by providing:

* Short and clean URLs
* User-owned link management
* Fast redirection
* Click tracking
* Link analytics
* Link activation/deactivation
* Expiration support
* Security controls against abuse
* A foundation for future scaling with Redis or async event processing

Example:

```text
Original URL:
https://example.com/products/category/spring-boot-course?ref=facebook_campaign_2026

Short URL:
https://linkflow.io/aB92xK
```

---

## Target Users

The platform is designed for:

* Marketing teams tracking campaign performance
* Social media managers sharing clean links
* SaaS platforms needing trackable links
* Businesses using branded links
* Internal teams managing shared resources
* Developers learning production-grade backend architecture

---

## Core MVP Features

The first version should focus on these features:

1. User registration
2. User login with JWT authentication
3. Create short links
4. Generate unique short codes
5. Redirect short links to original URLs
6. List the authenticated user's links
7. Update link metadata, such as title or expiration date
8. Activate or deactivate links
9. Track total click count
10. Store basic click events
11. Provide basic analytics
12. Support pagination, sorting, and filtering
13. Use Flyway for database migration
14. Use global exception handling
15. Return consistent API responses

---

## Non-MVP Features for Future Versions

Do not implement these in the first version unless the MVP is already stable:

* Redis caching for high-speed redirects
* Redis or Kafka-based async click tracking
* QR code generation
* Custom aliases
* Custom domains
* Team or workspace support
* Admin moderation panel
* Suspicious URL detection
* Blocked domains
* API keys for external integrations
* Advanced analytics dashboard
* Geolocation analytics
* Browser/device analytics
* CI/CD pipeline
* Observability with logs, metrics, and tracing

---

## Main Domain Concepts

### User

A user owns links.

A user can:

* Register
* Login
* Create links
* Manage their own links
* View analytics for their own links

### Link

A link represents a shortened URL.

A link should contain:

* ID
* Owner user ID
* Original URL
* Short code
* Optional title
* Click count
* Active status
* Optional expiration date
* Created timestamp
* Updated timestamp

### Click Event

A click event represents one access to a short link.

A click event may contain:

* ID
* Link ID
* Clicked timestamp
* IP address
* User agent
* Referer
* Browser
* Device
* Country

For the MVP, it is acceptable to store only basic click event data.

---

## Recommended Database Tables

The initial database should contain:

```text
users
links
click_events
```

Relationship:

```text
users 1 ──── * links
links 1 ──── * click_events
```

Meaning:

* One user can create many links.
* One link can have many click events.

---

## Database Migration Rule

Use **Flyway** as the source of truth for database schema.

Do not rely on Hibernate auto-DDL to create or update production tables.

Recommended configuration:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Flyway migration files should be placed in:

```text
src/main/resources/db/migration
```

Example:

```text
V1__create_users_table.sql
V2__create_links_table.sql
V3__create_click_events_table.sql
```

---

## Recommended Backend Architecture

Use package-by-feature architecture, not only package-by-layer.

Recommended structure:

```text
com.giabao.linkflow
├── auth
├── user
├── link
├── redirect
├── analytics
├── admin
├── common
├── security
└── config
```

Each feature package can contain its own:

```text
controller
service
repository
dto
mapper
entity
```

Example:

```text
link
├── LinkController.java
├── LinkService.java
├── LinkServiceImpl.java
├── LinkRepository.java
├── Link.java
├── dto
│   ├── CreateLinkRequest.java
│   ├── UpdateLinkRequest.java
│   └── LinkResponse.java
└── mapper
    └── LinkMapper.java
```

---

## Code Style Rules

Follow clean code principles.

The coding agent must follow these rules:

1. Keep controllers thin.
2. Put business logic inside services.
3. Do not expose entities directly in API responses.
4. Use DTOs for request and response models.
5. Validate request DTOs with Jakarta Bean Validation.
6. Use constructor injection.
7. Avoid field injection.
8. Avoid large service methods.
9. Avoid duplicated logic.
10. Keep method names clear and intention-revealing.
11. Use transactions properly.
12. Do not swallow exceptions silently.
13. Use meaningful custom exceptions.
14. Use pagination for list APIs.
15. Avoid returning raw `Map<String, Object>` unless there is a strong reason.
16. Avoid magic strings and magic numbers.
17. Prefer enums for fixed business values.
18. Keep security logic centralized.
19. Keep API response format consistent.
20. Write code that is easy to test.

---

## Security Requirements

Security is a core part of this project.

The platform should support:

* Password hashing with BCrypt
* JWT-based authentication
* Role-based authorization
* User ownership validation
* Protected APIs for authenticated users
* Public redirect endpoint for short links
* Validation against invalid URLs
* Basic abuse prevention readiness

Important rule:

A user must never be able to access, update, delete, or view analytics for another user's links.

Ownership checks are mandatory.

Example:

```text
User A owns Link A.
User B must not be able to update, delete, or view analytics for Link A.
```

---

## Authentication Direction

Use JWT authentication.

Recommended flow:

```text
User logs in
        ↓
Backend verifies email and password
        ↓
Backend generates JWT access token
        ↓
Client sends token in Authorization header
        ↓
Backend parses token
        ↓
Backend stores authenticated user in SecurityContextHolder
        ↓
Services use current user ID for ownership checks
```

The JWT subject should preferably contain the user ID.

Example:

```text
sub = userId
```

---

## Redirect Flow

The redirect endpoint should be public.

Example:

```http
GET /{shortCode}
```

Flow:

```text
User opens short URL
        ↓
Backend receives shortCode
        ↓
Find link by shortCode
        ↓
Check if link exists
        ↓
Check if link is active
        ↓
Check if link is not expired
        ↓
Increase click count
        ↓
Record click event
        ↓
Redirect to original URL
```

For MVP, direct database access is acceptable.

For future scaling, Redis cache can be added:

```text
shortCode -> originalUrl
```

---

## Analytics Direction

MVP analytics can include:

* Total clicks
* Clicks by day
* Latest click events
* Click count per link

Future analytics may include:

* Browser statistics
* Device statistics
* Country statistics
* Referrer statistics
* Campaign performance

Do not over-engineer analytics in the MVP.

---

## Short Code Generation Requirements

Short codes must be unique.

A short code should be:

* Short
* URL-safe
* Random or deterministic
* Hard to guess
* Collision-safe

Recommended MVP approach:

1. Generate a random Base62 code.
2. Check uniqueness using the database unique constraint.
3. Retry when a collision occurs.
4. Keep a maximum retry limit.
5. Throw a clear exception if generation fails.

Example characters:

```text
abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789
```

Do not use confusing or unsafe characters in URLs.

---

## API Response Format

Use a consistent response format.

Example success response:

```json
{
  "timestamp": "2026-06-07T10:00:00Z",
  "status": 200,
  "message": "Success",
  "data": {}
}
```

Example error response:

```json
{
  "timestamp": "2026-06-07T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid original URL",
  "path": "/api/v1/links"
}
```

Do not return inconsistent response shapes across endpoints.

---

## Suggested REST API Design

### Auth APIs

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### Link APIs

```http
POST /api/v1/links
GET /api/v1/links
GET /api/v1/links/{id}
PATCH /api/v1/links/{id}
DELETE /api/v1/links/{id}
PATCH /api/v1/links/{id}/activate
PATCH /api/v1/links/{id}/deactivate
```

### Redirect API

```http
GET /{shortCode}
```

### Analytics APIs

```http
GET /api/v1/links/{id}/analytics
GET /api/v1/links/{id}/click-events
```

---

## Validation Rules

### User Registration

Validate:

* Email is required
* Email format is valid
* Password is required
* Password has minimum length
* Email must be unique

### Link Creation

Validate:

* Original URL is required
* Original URL must be valid
* Title length must be limited
* Expiration date must be in the future if provided

### Short Code

Validate:

* Short code must be unique
* Short code must contain only allowed characters
* Short code length must be limited

---

## Error Handling Rules

Use global exception handling with `@RestControllerAdvice`.

Common exceptions:

* UserNotFoundException
* EmailAlreadyExistsException
* InvalidCredentialsException
* LinkNotFoundException
* LinkExpiredException
* LinkInactiveException
* AccessDeniedException
* ShortCodeGenerationException
* InvalidUrlException

Do not expose stack traces to clients.

---

## Transaction Rules

Use `@Transactional` on service methods that modify data.

Examples:

```java
@Transactional
public LinkResponse createLink(CreateLinkRequest request) {
    // business logic
}
```

Use read-only transactions for query methods:

```java
@Transactional(readOnly = true)
public Page<LinkResponse> getMyLinks(Pageable pageable) {
    // query logic
}
```

---

## Performance Rules

The agent should design with performance in mind.

Important points:

* Add indexes for frequently queried columns.
* Index `links.short_code`.
* Index `links.user_id`.
* Index `click_events.link_id`.
* Avoid N+1 queries.
* Use pagination for large lists.
* Do not load unnecessary relationships eagerly.
* Prefer lazy relationships by default.
* Use fetch joins or EntityGraph only when needed.
* Keep redirect flow fast.

---

## Recommended PostgreSQL Indexes

Recommended indexes:

```sql
CREATE UNIQUE INDEX idx_links_short_code ON links(short_code);
CREATE INDEX idx_links_user_id ON links(user_id);
CREATE INDEX idx_click_events_link_id ON click_events(link_id);
CREATE INDEX idx_click_events_clicked_at ON click_events(clicked_at);
```

---

## Entity Design Rules

Use UUID as the primary key for main business tables.

Recommended:

```java
@Id
@GeneratedValue
private UUID id;
```

Use auditing fields:

```java
createdAt
updatedAt
```

Enable JPA auditing with:

```java
@EnableJpaAuditing
```

Avoid exposing entity classes directly to the frontend.

---

## Role Design

Use enum for roles in Java.

Example:

```java
public enum UserRole {
    USER,
    ADMIN
}
```

Store role as string in the database.

Example:

```java
@Enumerated(EnumType.STRING)
private UserRole role;
```

Database column:

```sql
role VARCHAR(50) NOT NULL DEFAULT 'USER'
```

Do not use ordinal enum mapping.

---

## Current Technology Stack

Use:

* Java 21
* Spring Boot 3.x
* Spring Web
* Spring Data JPA
* Spring Security
* PostgreSQL
* Flyway
* Lombok
* MapStruct
* Jakarta Bean Validation
* Docker / Docker Compose

Future optional technologies:

* Redis
* Kafka or Redis Stream
* Swagger/OpenAPI
* GitHub Actions
* Prometheus/Grafana
* Testcontainers

---

## Development Priorities

The correct implementation order is:

1. Project setup
2. PostgreSQL and Docker Compose
3. Flyway migration
4. User entity and repository
5. Authentication and JWT
6. Link entity and repository
7. Create short link API
8. Redirect API
9. Link management APIs
10. Click count tracking
11. Click event tracking
12. Basic analytics
13. Pagination and filtering
14. Global exception handling
15. API response standardization
16. Security hardening
17. Testing
18. Documentation

Do not start with advanced Redis/Kafka architecture before the MVP works correctly.

---

## Testing Requirements

At minimum, write tests for:

* Short code generation
* User registration
* Login flow
* Create link
* Redirect active link
* Reject inactive link
* Reject expired link
* Prevent users from accessing other users' links
* Link pagination
* Basic analytics

Recommended test types:

* Unit tests for pure business logic
* Integration tests for repositories
* Controller tests for API behavior
* Security tests for protected endpoints

---

## Documentation Requirements

The project should include a clear `README.md`.

The README should explain:

* What the project does
* Main features
* Tech stack
* How to run locally
* Environment variables
* Database migration
* API examples
* Future improvements

Optional:

* Swagger/OpenAPI documentation
* Architecture diagram
* Database diagram
* Sequence diagram for redirect flow

---

## Important Design Principle

This project must be built as a backend engineering portfolio project.

Do not make it look like a simple tutorial URL shortener.

The project should demonstrate:

* Clean architecture
* Secure authentication
* Ownership-based authorization
* Database migration discipline
* Scalable redirect design
* Analytics event modeling
* Production-ready API design
* Maintainable package structure
* Clear error handling
* Strong validation

---

## What the Agent Must Avoid

Avoid:

* Creating all code in one package
* Returning JPA entities directly
* Using Hibernate auto-DDL as the main schema tool
* Ignoring ownership checks
* Making redirect APIs require login
* Writing business logic in controllers
* Skipping validation
* Skipping global exception handling
* Creating inconsistent API responses
* Adding Redis/Kafka too early
* Over-engineering before MVP completion
* Using ordinal enum mapping
* Silently catching exceptions
* Hardcoding user IDs
* Allowing users to update other users' links

---

## Final Direction

Build LinkFlow Platform step by step.

The MVP should be simple but clean.

The final version should be strong enough to put on a Backend Java Spring Boot CV.

The project should prove that the developer understands not only CRUD, but also authentication, authorization, database design, fast redirects, analytics, caching readiness, scalability, and production-level backend engineering.
