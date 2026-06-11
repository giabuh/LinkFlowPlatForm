# Link Module — CRUD + Redirect (Design)

**Date:** 2026-06-11
**Status:** Approved
**Scope:** Link CRUD (owner-scoped) + short-code generation + public redirect. Click events and analytics are explicitly **out of scope** for this slice.

---

## 1. Goal & Rationale

The Link module is the core of LinkFlow Platform. After this slice:

- An authenticated user can **create, read, update, delete, and list their own links**.
- The system generates a **unique, URL-safe short code** per link.
- A public visitor hitting `GET /{shortCode}` is **302-redirected** to the original URL, and the link's `clickCount` is incremented.

Click event recording (`click_events` table) and analytics read APIs are deferred to a later slice.

---

## 2. Package Structure

Package-by-feature, mirroring the existing `user` module conventions.

```
com.giabao.linkflow.link
├── controller/LinkController            # /api/v1/links  (protected)
├── service/LinkService                  # interface
│   └── impl/LinkServiceImpl
├── service/ShortCodeGenerator           # Base62 + collision retry
├── repository/LinkRepository
├── specification/LinkSpecifications     # filter; ALWAYS scoped by userId
├── dto/request/CreateLinkRequest
├── dto/request/UpdateLinkRequest
├── dto/request/LinkFilterRequest        # extends PageableRequest
├── dto/response/LinkResponse
└── mapper/LinkMapper

com.giabao.linkflow.redirect
└── controller/RedirectController        # GET /{shortCode}  (public)
```

The `Link` entity already exists at `com.giabao.linkflow.link.entity.Link` and is unchanged.

### Companion change: normalize User API prefix

`UserController` currently maps to `/users` while `AuthController` already uses `/api/v1/auth`. As part of this work, move `UserController` to **`/api/v1/users`** for consistency, and update:

- `UserController` `@RequestMapping("/api/v1/users")`
- `SecurityConfig` line 44: `"/users/**"` → `"/api/v1/users/**"` (still `hasRole("ADMIN")`)
- `UserControllerTest` request paths

No other behavior changes.

---

## 3. API Surface

| Method | Path | Access | Purpose |
|--------|------|--------|---------|
| POST | `/api/v1/links` | owner | Create link; generate `shortCode` |
| GET | `/api/v1/links` | owner | List caller's links (page/sort/filter) |
| GET | `/api/v1/links/{id}` | owner | Get one link |
| PUT | `/api/v1/links/{id}` | owner | Update `originalUrl`, `title`, `expiresAt` |
| PATCH | `/api/v1/links/{id}/activate` | owner | Set `active = true` |
| PATCH | `/api/v1/links/{id}/deactivate` | owner | Set `active = false` |
| DELETE | `/api/v1/links/{id}` | owner | Delete link |
| GET | `/{shortCode}` | public | 302 redirect + increment click |

**Decision — PUT over PATCH for update:** the existing `UserController` uses `PUT` for full update; this module follows that for codebase consistency rather than AGENTS.md's suggested `PATCH`. `active` is intentionally **not** editable via the update body — it is controlled only through the dedicated `activate`/`deactivate` endpoints. `shortCode`, `clickCount`, and `user` are server-managed and never client-editable.

All `/api/v1/links/**` routes are protected (require authentication) via the existing `anyRequest().authenticated()` rule. No `SecurityConfig` change is needed for them.

---

## 4. DTOs & Validation

### CreateLinkRequest (record)
- `originalUrl` — `@NotBlank` + `@org.hibernate.validator.constraints.URL` (Hibernate Validator is already on the classpath). Reject blank/invalid; no hard length cap (TEXT column).
- `title` — optional, max 255.
- `expiresAt` — optional `Instant`; if present must be in the future (`@Future`).

### UpdateLinkRequest (record)
- Same fields as create (`originalUrl`, `title`, `expiresAt`); same validation. Full replacement semantics (PUT).

### LinkFilterRequest (extends `PageableRequest`)
- `ALLOWED_SORT_FIELDS = { "createdAt", "updatedAt", "clickCount", "title" }`
- `DEFAULT_SORT_BY = "createdAt"`
- Filters: `title` (contains, case-insensitive), `active` (Boolean, nullable).
- `user_id = currentUser` is **always** applied server-side and is **not** a client-supplied filter field.

### LinkResponse (record)
`id, originalUrl, shortCode, title, clickCount, active, expiresAt, createdAt, updatedAt`. The `user` relation is **not** exposed (caller already knows it's theirs).

---

## 5. Ownership Enforcement (core security)

Every single-link operation (`get/update/activate/deactivate/delete`) resolves the link by **`id` AND `currentUserId` together**. If no match:

- Throw `AppException(ErrorCode.NOT_FOUND, ...)` — **not 403** — so we never leak the existence of another user's link.

List and filter always add `user_id = currentUserId` in `LinkSpecifications`, so a user can never page through another user's links.

`currentUserId` is obtained via `CurrentUserService.getCurrentUser().getId()`.

---

## 6. Short Code Generation (`ShortCodeGenerator`)

- Alphabet: Base62 `[A-Za-z0-9]`.
- Length: **7** characters (within the `[6,32]` range already permitted by `SecurityConfig`'s redirect regex `^/[A-Za-z0-9]{6,32}$`).
- Use a cryptographically adequate random source (`SecureRandom`).
- Algorithm: generate → `linkRepository.existsByShortCode(code)` → on collision, retry; **max 5 attempts**; if all collide, throw `AppException` mapping to HTTP 500 with a meaningful message.
- The DB `UNIQUE` constraint on `links.short_code` is the final safety net against the race between check and insert (a `DataIntegrityViolationException` on save is treated as a collision retry as well).

---

## 7. Redirect Flow (`RedirectController`, `GET /{shortCode}`)

```
receive shortCode
  → linkRepository.findByShortCode(shortCode)
  → absent              → 404
  → active == false     → 404
  → expiresAt in past   → 404
  → increment click_count via atomic UPDATE (does NOT touch updated_at)
  → 302 Found, Location: originalUrl
```

- Click increment uses a `@Modifying @Query` bulk update (`UPDATE Link l SET l.clickCount = l.clickCount + 1 WHERE l.id = :id`) so concurrent redirects are race-free and JPA auditing does **not** bump `updatedAt`.
- A bad/expired/inactive short code returns a **plain 404** (browser-facing redirect, not a REST/JSON client), bypassing the `ApiResponse` envelope.
- The route is already `permitAll` in `SecurityConfig` (regex `^/[A-Za-z0-9]{6,32}$`, GET). No security change required.

---

## 8. Persistence

- The `links` table and its indexes (`idx_links_short_code` unique-ish, `idx_links_user_id`, `idx_links_created_at`) already exist in `V1__init_schema.sql`. **No new Flyway migration** is required for this slice.
- `LinkRepository extends JpaRepository<Link, UUID>, JpaSpecificationExecutor<Link>` with:
  - `Optional<Link> findByShortCode(String shortCode)`
  - `boolean existsByShortCode(String shortCode)`
  - `Optional<Link> findByIdAndUserId(UUID id, UUID userId)`
  - `@Modifying int incrementClickCount(UUID id)`
- Write operations use `@Transactional`; reads use `@Transactional(readOnly = true)`, matching the project's clean-code rules.

---

## 9. Testing

Following existing test patterns (`UserServiceImplTest`, `UserControllerTest`, security tests):

- **`ShortCodeGeneratorTest`** — output is 7-char Base62; retries on `existsByShortCode` collision; throws after max attempts.
- **`LinkServiceImplTest`** — create (shortCode assigned, owner set from current user), get/update/delete happy paths; **ownership**: another user's link id resolves to `NOT_FOUND` for get/update/activate/deactivate/delete; activate/deactivate toggles `active`; list is scoped to current user.
- **`LinkControllerTest`** — request validation (blank/invalid URL, past `expiresAt`), status codes (201 create; 200 get/list/update; 200 delete via `ResponseEntity.ok().build()`, matching `UserController`), `@ApiMessage` envelope.
- **`RedirectControllerTest`** — 302 + correct `Location` on happy path; 404 when missing / inactive / expired; `clickCount` incremented exactly once.

---

## 10. Out of Scope (next slices)

- `click_events` table (V2 migration) and per-click event recording.
- Analytics read APIs (`/api/v1/links/{id}/analytics`, `/click-events`).
- Redis caching of `shortCode -> originalUrl`.
- Custom aliases, QR codes, custom domains.
