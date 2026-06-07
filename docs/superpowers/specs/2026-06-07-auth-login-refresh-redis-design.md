# Auth Login, Access Token, Refresh Token, and Redis Session Design

## Context

LinkFlowPlatform already has user persistence, BCrypt password hashing, Flyway-managed `users` and `links` tables, a consistent API response wrapper, and Spring Security on the classpath. It does not yet have an auth module, JWT token service, Redis integration, or a security filter chain that authenticates API requests.

This design adds production-style MVP authentication while keeping the scope focused: Spring Security handles login verification and protected request authentication, access tokens remain stateless JWTs, and Redis stores hashed refresh-token session state for rotation and logout.

## Goals

- Add public auth endpoints under `/api/v1/auth`.
- Use Spring Security for credential authentication and protected API authorization.
- Use access tokens with `sub = userId` so clients do not send `userId` in normal user APIs.
- Populate `SecurityContextHolder` from a verified access token on protected requests.
- Support multi-device login sessions.
- Store refresh session state in Redis, but store only a hash of the refresh token.
- Send refresh tokens in an HttpOnly cookie.
- Rotate refresh tokens on every refresh.
- Keep existing `/users/**` as admin-only user management.

## Non-Goals

- Do not store access tokens in Redis for MVP.
- Do not implement logout-all or session listing yet.
- Do not add Redis cache for redirect URLs yet.
- Do not implement custom domains, API keys, QR codes, or advanced analytics as part of auth.
- Do not manually Base64-decode JWT payloads and trust claims without signature verification.

## Recommended Approach

Use Spring Security plus Redis-backed refresh sessions:

- `AuthenticationManager` and `DaoAuthenticationProvider` verify login credentials.
- `JwtAuthenticationFilter` authenticates protected requests from `Authorization: Bearer <accessToken>`.
- `TokenService` creates and verifies JWTs with JJWT using the configured secret.
- Redis stores one refresh session per login session id.
- Refresh token rotation updates the Redis hash and refresh cookie.

This approach is stronger than simple stateless JWT refresh tokens while avoiding full session-platform complexity before the MVP link and analytics features are complete.

## API Contract

### Public Auth APIs

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

`POST /api/v1/auth/register` creates a user and returns `UserResponse`. Register does not auto-login. The user calls login explicitly to create a session.

`POST /api/v1/auth/login` accepts email and password. The service delegates credential verification to `AuthenticationManager`. On success it returns an access token in the response body and sets a refresh token cookie.

`POST /api/v1/auth/refresh` reads the refresh token from the HttpOnly cookie, validates it, compares its hash with Redis, rotates the refresh token, returns a new access token, and sets a new refresh cookie.

`POST /api/v1/auth/logout` logs out the current session only. It deletes the Redis refresh session if a session id can be extracted, clears the refresh cookie, and returns success.

### Existing User APIs

Existing `/users/**` endpoints remain but become admin-only APIs. They should not be used as public registration endpoints after `/api/v1/auth/register` exists.

## Security Filter Chain

Configure the application as a stateless REST API:

- `SessionCreationPolicy.STATELESS`
- CSRF disabled for the MVP API design
- `JwtAuthenticationFilter` added before `UsernamePasswordAuthenticationFilter`

Authorization rules:

```text
Public:
- POST /api/v1/auth/register
- POST /api/v1/auth/login
- POST /api/v1/auth/refresh
- POST /api/v1/auth/logout
- GET /{shortCode}

Admin only:
- /users/** requires ROLE_ADMIN

Authenticated:
- other protected application APIs, including future /api/v1/links/** and analytics endpoints
```

## Login Flow

```text
Client sends email/password
        ↓
AuthController.login()
        ↓
AuthService.login()
        ↓
AuthenticationManager.authenticate(UsernamePasswordAuthenticationToken)
        ↓
DaoAuthenticationProvider
        ↓
CustomUserDetailsService.loadUserByUsername(email)
        ↓
PasswordEncoder.matches(rawPassword, passwordHash)
        ↓
Authentication succeeds
        ↓
AuthService reads CustomUserDetails from Authentication principal
        ↓
Create sessionId
        ↓
Create access token and refresh token
        ↓
Hash refresh token and save Redis session
        ↓
Return access token body and Set-Cookie refresh token
```

The login request does not need to set `SecurityContextHolder`. In a stateless API, the context is populated by the JWT filter on later protected requests.

## Protected Request Flow

```text
Client sends Authorization: Bearer <accessToken>
        ↓
SecurityFilterChain
        ↓
JwtAuthenticationFilter
        ↓
Extract Bearer token
        ↓
TokenService verifies signature, expiry, and type=ACCESS with JJWT
        ↓
Read sub as user UUID
        ↓
CustomUserDetailsService.loadUserById(userId)
        ↓
Create UsernamePasswordAuthenticationToken
        ↓
SecurityContextHolder.getContext().setAuthentication(authentication)
        ↓
Controller and service code can read current user from SecurityContextHolder
```

User-owned APIs must not accept `userId` from request bodies for normal user actions. Services should derive identity from the authenticated principal. Future link APIs should create, list, update, delete, and analyze links using the current principal's user id.

## Token Design

### Access Token

Claims:

```text
sub: user UUID
role: USER | ADMIN
type: ACCESS
iat: issued at
exp: short expiry from config
```

Access tokens are sent by clients in the `Authorization` header. They are not stored in Redis for MVP. They are accepted only if JJWT verifies the signature and expiry and the token has `type=ACCESS`.

### Refresh Token

Claims:

```text
sub: user UUID
sid: session UUID
type: REFRESH
iat: issued at
exp: refresh expiry from config
```

Refresh tokens are stored on the client only as an HttpOnly cookie. A refresh token sent as a Bearer token must be rejected because `type != ACCESS`.

## JWT Parsing and Verification

JWTs must be parsed and verified with a library, not manual decoding. The design uses JJWT.

`TokenService` responsibilities:

- Create access tokens.
- Create refresh tokens.
- Parse and validate access tokens.
- Parse and validate refresh tokens.
- Verify signature using the secret from `app.jwt.secret`.
- Verify expiration.
- Verify token type.
- Parse `sub` and `sid` as UUIDs.

Representative JJWT verification shape:

```java
Claims claims = Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
```

The filter is allowed to manually extract the `Bearer ` prefix from the header, but it must delegate token validation and claim extraction to `TokenService`.

## Redis Refresh Session Design

Redis key:

```text
auth:refresh-session:{sessionId}
```

Value fields:

```json
{
  "userId": "uuid",
  "refreshTokenHash": "hash",
  "createdAt": "timestamp",
  "expiresAt": "timestamp"
}
```

The Redis key TTL matches the refresh token expiration. Redis stores only a hash of the refresh token, not the raw token.

Prefer HMAC-SHA256 for the refresh token hash if a separate hash secret is available. Plain SHA-256 is acceptable for MVP because JWT refresh tokens are high-entropy signed values, but HMAC makes the security posture clearer.

## Refresh Flow

```text
Read refresh_token cookie
        ↓
TokenService verifies signature, expiry, and type=REFRESH with JJWT
        ↓
Extract sub=userId and sid=sessionId
        ↓
Load Redis key auth:refresh-session:{sid}
        ↓
If missing, return 401
        ↓
Hash incoming refresh token
        ↓
Compare with Redis refreshTokenHash
        ↓
If mismatch, delete this Redis session, clear cookie, return 401
        ↓
If match, create new access token and new refresh token
        ↓
Store new refresh token hash in same Redis key with refreshed TTL
        ↓
Set new refresh cookie
        ↓
Return new access token body
```

Refresh token reuse or hash mismatch revokes only the current session for MVP. It does not revoke every user session.

## Cookie Design

MVP local-development cookie defaults:

```text
Name: refresh_token
HttpOnly: true
Secure: false
SameSite: Lax
Path: /api/v1/auth/refresh
Max-Age: refresh token expiration
```

Cookie settings should be configurable so production can use `Secure=true` and a stricter cross-site policy if a separate frontend origin requires it.

## Main Classes

Auth package:

```text
com.giabao.linkflow.auth.controller.AuthController
com.giabao.linkflow.auth.service.AuthService
com.giabao.linkflow.auth.service.TokenService
com.giabao.linkflow.auth.service.RefreshSessionService
com.giabao.linkflow.auth.service.RefreshCookieService
com.giabao.linkflow.auth.dto.req.RegisterRequest
com.giabao.linkflow.auth.dto.req.LoginRequest
com.giabao.linkflow.auth.dto.res.AuthResponse
com.giabao.linkflow.auth.dto.res.TokenResponse
```

Security package:

```text
com.giabao.linkflow.security.SecurityConfig
com.giabao.linkflow.security.JwtAuthenticationFilter
com.giabao.linkflow.security.CustomUserDetails
com.giabao.linkflow.security.CustomUserDetailsService
com.giabao.linkflow.security.CurrentUserService
```

`CustomUserDetails` implements `UserDetails` and wraps the domain `User`. It exposes `getId()`, `getEmail()`, `getRole()`, Spring authorities, username as email, and password as the stored password hash.

`CustomUserDetailsService` implements `UserDetailsService` for username/email login and also provides `loadUserById(UUID id)` for JWT filter authentication.

`CurrentUserService` centralizes reading the current `CustomUserDetails` from `SecurityContextHolder` so feature services do not duplicate security-context parsing.

## Configuration and Dependencies

Configuration shape:

```yaml
app:
  jwt:
    secret: "..."
    access-token-expiration-ms: 900000
    refresh-token-expiration-ms: 604800000
  auth:
    refresh-cookie:
      name: refresh_token
      http-only: true
      secure: false
      same-site: Lax
      path: /api/v1/auth/refresh

spring:
  data:
    redis:
      host: localhost
      port: 6379
```

Dependencies to add:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-redis")
implementation("io.jsonwebtoken:jjwt-api:0.13.0")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
```

`docker-compose.yml` should add a Redis service for local development.

## Error Handling

- Invalid login credentials: `401 UNAUTHORIZED`.
- Missing, invalid, expired, or wrong-type access token on protected API: `401 UNAUTHORIZED`.
- Missing refresh cookie: `401 UNAUTHORIZED`.
- Expired refresh token: `401 UNAUTHORIZED`, clear cookie when possible.
- Redis refresh session missing: `401 UNAUTHORIZED`.
- Refresh token hash mismatch or reuse: delete current session, clear cookie, return `401 UNAUTHORIZED`.
- Accessing `/users/**` without admin role: `403 FORBIDDEN`.
- Duplicate register email: `409 CONFLICT`.
- Invalid request DTOs: `400 INVALID_ARGUMENT`.

Spring Security authentication and access-denied failures should return the project's consistent `ApiResponse` envelope.

## Test Strategy

Controller and service tests should cover:

- Register success, validation failure, and duplicate email.
- Login success returns access token and `Set-Cookie`.
- Login invalid credentials returns `401`.
- Refresh success rotates refresh token and returns a new access token.
- Refresh missing cookie returns `401`.
- Refresh token hash mismatch deletes the current Redis session and returns `401`.
- Logout clears the cookie and deletes current session when possible.
- Token service creates access tokens with `sub=userId`.
- Token service rejects refresh tokens where access tokens are expected.
- Token service rejects expired or invalid JWTs.
- JWT filter sets an authenticated `CustomUserDetails` principal for valid Bearer access tokens.
- Protected APIs reject missing or invalid Bearer tokens.
- `/users/**` requires `ROLE_ADMIN`.

Existing `/users` controller slice tests can keep filters disabled where they only test controller validation and response shape. Security integration tests should explicitly verify the new authorization rules.

## Implementation Order

1. Add dependencies and Redis local config.
2. Add auth and cookie configuration properties.
3. Add `CustomUserDetails` and `CustomUserDetailsService`.
4. Add `TokenService` with JJWT token creation and verified parsing.
5. Add `RefreshSessionService` with hashed Redis refresh sessions.
6. Add `RefreshCookieService`.
7. Add `AuthService` and `AuthController`.
8. Add `JwtAuthenticationFilter` and `SecurityConfig`.
9. Convert `/users/**` to admin-only through security config.
10. Add tests for token, auth, refresh session, filter, and authorization behavior.

## Remaining Risks

- Cookie behavior in local tools can differ from browser behavior; endpoint tests must assert cookie attributes directly.
- Production cross-origin frontend support may require `Secure=true`, `SameSite=None`, and CORS credentials. This is intentionally configurable but not the MVP default.
- If the JWT secret remains a plain text string, it must be at least 32 bytes for HS256. A Base64 secret property can be introduced later if desired.
- Logout-all and session inventory require a user-session index in Redis and are intentionally deferred.
