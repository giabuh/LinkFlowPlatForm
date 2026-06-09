# Auth Login Refresh Redis Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Spring Security login, Bearer access-token authentication, HttpOnly refresh-token rotation, and Redis-backed refresh sessions.

**Architecture:** Login uses `AuthenticationManager` with a `DaoAuthenticationProvider`. Protected requests use a custom `JwtAuthenticationFilter` that verifies access tokens with JJWT, loads `CustomUserDetails` by JWT subject, and populates `SecurityContextHolder`. Refresh tokens are JWTs stored in HttpOnly cookies, while Redis stores only hashed refresh-token sessions by session id.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Security, Spring Data Redis, JJWT 0.13.0, PostgreSQL, Flyway, JUnit 5, Mockito, MockMvc.

**Commit Policy:** Do not commit during implementation unless the user explicitly asks for it. Use `git diff`, `git status`, and test output as checkpoints.

---

## File Structure

Create or modify these files only unless implementation reveals a concrete blocker:

- Modify `build.gradle.kts` to add Redis and JJWT dependencies.
- Modify `docker-compose.yml` to add local Redis.
- Modify `src/main/resources/application.yaml` for Redis and refresh-cookie config.
- Modify `src/main/java/com/giabao/linkflow/user/repository/UserRepository.java` to add email lookup.
- Create `src/main/java/com/giabao/linkflow/auth/config/JwtProperties.java`.
- Create `src/main/java/com/giabao/linkflow/auth/config/RefreshCookieProperties.java`.
- Create `src/main/java/com/giabao/linkflow/auth/dto/req/RegisterRequest.java`.
- Create `src/main/java/com/giabao/linkflow/auth/dto/req/LoginRequest.java`.
- Create `src/main/java/com/giabao/linkflow/auth/dto/res/TokenResponse.java`.
- Create `src/main/java/com/giabao/linkflow/auth/dto/res/AuthResponse.java`.
- Create `src/main/java/com/giabao/linkflow/auth/service/TokenType.java`.
- Create `src/main/java/com/giabao/linkflow/auth/service/ParsedAccessToken.java`.
- Create `src/main/java/com/giabao/linkflow/auth/service/ParsedRefreshToken.java`.
- Create `src/main/java/com/giabao/linkflow/auth/service/TokenService.java`.
- Create `src/main/java/com/giabao/linkflow/auth/service/RefreshSession.java`.
- Create `src/main/java/com/giabao/linkflow/auth/service/RefreshSessionService.java`.
- Create `src/main/java/com/giabao/linkflow/auth/service/RefreshCookieService.java`.
- Create `src/main/java/com/giabao/linkflow/auth/service/AuthService.java`.
- Create `src/main/java/com/giabao/linkflow/auth/controller/AuthController.java`.
- Create `src/main/java/com/giabao/linkflow/security/CustomUserDetails.java`.
- Create `src/main/java/com/giabao/linkflow/security/CustomUserDetailsService.java`.
- Create `src/main/java/com/giabao/linkflow/security/CurrentUserService.java`.
- Create `src/main/java/com/giabao/linkflow/security/JwtAuthenticationFilter.java`.
- Create `src/main/java/com/giabao/linkflow/security/RestAuthenticationEntryPoint.java`.
- Create `src/main/java/com/giabao/linkflow/security/RestAccessDeniedHandler.java`.
- Create `src/main/java/com/giabao/linkflow/security/SecurityConfig.java`.
- Create tests under `src/test/java/com/giabao/linkflow/auth/...` and `src/test/java/com/giabao/linkflow/security/...`.

---

### Task 1: Dependencies and Local Redis Config

**Files:**
- Modify: `build.gradle.kts`
- Modify: `docker-compose.yml`
- Modify: `src/main/resources/application.yaml`

- [ ] **Step 1: Add dependencies**

In `build.gradle.kts`, add these dependencies near the existing Spring Boot starters and runtime dependencies:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-redis")
implementation("io.jsonwebtoken:jjwt-api:0.13.0")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
```

- [ ] **Step 2: Add Redis to Docker Compose**

In `docker-compose.yml`, add a Redis service beside `postgres`:

```yaml
  redis:
    image: redis:7-alpine
    container_name: linkflow-redis
    ports:
      - "6379:6379"
    volumes:
      - linkflow_redis_data:/data
```

Also add this volume under `volumes:`:

```yaml
  linkflow_redis_data:
```

- [ ] **Step 3: Add Redis and cookie properties**

In `src/main/resources/application.yaml`, extend existing config:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
app:
  jwt:
    secret: "r5CDPzaAXNoDqplVpCywWUyrwwigutUMUEPVKLDzMl2"
    access-token-expiration-ms: 900000 # 15m
    refresh-token-expiration-ms: 604800000 # 7d
  auth:
    refresh-cookie:
      name: refresh_token
      http-only: true
      secure: false
      same-site: Lax
      path: /api/v1/auth/refresh
```

Preserve existing datasource, JPA, and Flyway settings. If `app.jwt` already exists, update its values instead of duplicating the block.

- [ ] **Step 4: Verify dependency resolution**

Run:

```bash
./gradlew dependencies --configuration runtimeClasspath
```

Expected: command completes successfully and output includes `spring-boot-starter-data-redis` and `jjwt-impl`.

- [ ] **Step 5: Checkpoint without commit**

Run:

```bash
git diff -- build.gradle.kts docker-compose.yml src/main/resources/application.yaml
git status --short
```

Expected: only the three intended files are changed for this task.

---

### Task 2: Auth Configuration Properties

**Files:**
- Create: `src/main/java/com/giabao/linkflow/auth/config/JwtProperties.java`
- Create: `src/main/java/com/giabao/linkflow/auth/config/RefreshCookieProperties.java`
- Modify: `src/main/java/com/giabao/linkflow/LinkFlowPlatformApplication.java`
- Test: `src/test/java/com/giabao/linkflow/auth/config/AuthPropertiesTest.java`

- [ ] **Step 1: Write configuration properties test**

Create `src/test/java/com/giabao/linkflow/auth/config/AuthPropertiesTest.java`:

```java
package com.giabao.linkflow.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "app.jwt.secret=12345678901234567890123456789012",
                    "app.jwt.access-token-expiration-ms=900000",
                    "app.jwt.refresh-token-expiration-ms=604800000",
                    "app.auth.refresh-cookie.name=refresh_token",
                    "app.auth.refresh-cookie.http-only=true",
                    "app.auth.refresh-cookie.secure=false",
                    "app.auth.refresh-cookie.same-site=Lax",
                    "app.auth.refresh-cookie.path=/api/v1/auth/refresh"
            );

    @Test
    void bindsJwtAndRefreshCookieProperties() {
        contextRunner.run(context -> {
            JwtProperties jwt = context.getBean(JwtProperties.class);
            RefreshCookieProperties cookie = context.getBean(RefreshCookieProperties.class);

            assertThat(jwt.secret()).isEqualTo("12345678901234567890123456789012");
            assertThat(jwt.accessTokenExpirationMs()).isEqualTo(900000);
            assertThat(jwt.refreshTokenExpirationMs()).isEqualTo(604800000);
            assertThat(cookie.name()).isEqualTo("refresh_token");
            assertThat(cookie.httpOnly()).isTrue();
            assertThat(cookie.secure()).isFalse();
            assertThat(cookie.sameSite()).isEqualTo("Lax");
            assertThat(cookie.path()).isEqualTo("/api/v1/auth/refresh");
        });
    }

    @EnableConfigurationProperties({JwtProperties.class, RefreshCookieProperties.class})
    static class TestConfig {
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
./gradlew test --tests com.giabao.linkflow.auth.config.AuthPropertiesTest
```

Expected: FAIL because `JwtProperties` and `RefreshCookieProperties` do not exist.

- [ ] **Step 3: Create `JwtProperties`**

Create `src/main/java/com/giabao/linkflow/auth/config/JwtProperties.java`:

```java
package com.giabao.linkflow.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationMs,
        long refreshTokenExpirationMs
) {
}
```

- [ ] **Step 4: Create `RefreshCookieProperties`**

Create `src/main/java/com/giabao/linkflow/auth/config/RefreshCookieProperties.java`:

```java
package com.giabao.linkflow.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.refresh-cookie")
public record RefreshCookieProperties(
        String name,
        boolean httpOnly,
        boolean secure,
        String sameSite,
        String path
) {
}
```

- [ ] **Step 5: Enable property scanning**

Modify `src/main/java/com/giabao/linkflow/LinkFlowPlatformApplication.java`:

```java
package com.giabao.linkflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LinkFlowPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkFlowPlatformApplication.class, args);
    }
}
```

- [ ] **Step 6: Run test and verify it passes**

Run:

```bash
./gradlew test --tests com.giabao.linkflow.auth.config.AuthPropertiesTest
```

Expected: PASS.

- [ ] **Step 7: Checkpoint without commit**

Run:

```bash
git diff -- src/main/java/com/giabao/linkflow/LinkFlowPlatformApplication.java src/main/java/com/giabao/linkflow/auth/config src/test/java/com/giabao/linkflow/auth/config
```

Expected: only configuration-property changes are shown.

---

### Task 3: Security Principal and User Lookup

**Files:**
- Modify: `src/main/java/com/giabao/linkflow/user/repository/UserRepository.java`
- Create: `src/main/java/com/giabao/linkflow/security/CustomUserDetails.java`
- Create: `src/main/java/com/giabao/linkflow/security/CustomUserDetailsService.java`
- Create: `src/main/java/com/giabao/linkflow/security/CurrentUserService.java`
- Test: `src/test/java/com/giabao/linkflow/security/CustomUserDetailsServiceTest.java`

- [ ] **Step 1: Write failing user-details tests**

Create `src/test/java/com/giabao/linkflow/security/CustomUserDetailsServiceTest.java`:

```java
package com.giabao.linkflow.security;

import com.giabao.linkflow.user.enity.User;
import com.giabao.linkflow.user.enity.UserRole;
import com.giabao.linkflow.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsernameReturnsCustomPrincipalWithRoleAuthority() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "ada@example.com", "hash", UserRole.ADMIN);
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

        CustomUserDetails details = (CustomUserDetails) service.loadUserByUsername("ada@example.com");

        assertThat(details.getId()).isEqualTo(userId);
        assertThat(details.getUsername()).isEqualTo("ada@example.com");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByIdReturnsCustomPrincipal() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "ada@example.com", "hash", UserRole.USER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        CustomUserDetails details = service.loadUserById(userId);

        assertThat(details.getId()).isEqualTo(userId);
        assertThat(details.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void loadUserByUsernameThrowsWhenMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

    private User user(UUID id, String email, String password, UserRole role) throws Exception {
        User user = new User();
        setId(user, id);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setFullName("Ada Lovelace");
        return user;
    }

    private void setId(User user, UUID id) throws Exception {
        Field field = user.getClass().getSuperclass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
./gradlew test --tests com.giabao.linkflow.security.CustomUserDetailsServiceTest
```

Expected: FAIL because security classes and `findByEmail` do not exist.

- [ ] **Step 3: Add email lookup to repository**

Modify `src/main/java/com/giabao/linkflow/user/repository/UserRepository.java`:

```java
package com.giabao.linkflow.user.repository;

import com.giabao.linkflow.user.enity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    Optional<User> findByEmail(String email);
}
```

- [ ] **Step 4: Create `CustomUserDetails`**

Create `src/main/java/com/giabao/linkflow/security/CustomUserDetails.java`:

```java
package com.giabao.linkflow.security;

import com.giabao.linkflow.user.enity.User;
import com.giabao.linkflow.user.enity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {
    private final UUID id;
    private final String email;
    private final String password;
    private final UserRole role;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
```

- [ ] **Step 5: Create `CustomUserDetailsService`**

Create `src/main/java/com/giabao/linkflow/security/CustomUserDetailsService.java`:

```java
package com.giabao.linkflow.security;

import com.giabao.linkflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private static final String USER_NOT_FOUND_MESSAGE = "User not found";

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = username == null ? null : username.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(email)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE));
    }

    public CustomUserDetails loadUserById(UUID id) {
        return userRepository.findById(id)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE));
    }
}
```

- [ ] **Step 6: Create `CurrentUserService`**

Create `src/main/java/com/giabao/linkflow/security/CurrentUserService.java`:

```java
package com.giabao.linkflow.security;

import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return userDetails;
    }
}
```

- [ ] **Step 7: Run test and verify it passes**

Run:

```bash
./gradlew test --tests com.giabao.linkflow.security.CustomUserDetailsServiceTest
```

Expected: PASS.

---

### Task 4: JJWT Token Service

**Files:**
- Create: `src/main/java/com/giabao/linkflow/auth/service/TokenType.java`
- Create: `src/main/java/com/giabao/linkflow/auth/service/ParsedAccessToken.java`
- Create: `src/main/java/com/giabao/linkflow/auth/service/ParsedRefreshToken.java`
- Create: `src/main/java/com/giabao/linkflow/auth/service/TokenService.java`
- Test: `src/test/java/com/giabao/linkflow/auth/service/TokenServiceTest.java`

- [ ] **Step 1: Write failing token service tests**

Create `src/test/java/com/giabao/linkflow/auth/service/TokenServiceTest.java`:

```java
package com.giabao.linkflow.auth.service;

import com.giabao.linkflow.auth.config.JwtProperties;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.security.CustomUserDetails;
import com.giabao.linkflow.user.enity.User;
import com.giabao.linkflow.user.enity.UserRole;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private final TokenService tokenService = new TokenService(new JwtProperties(
            "12345678901234567890123456789012",
            900000,
            604800000
    ));

    @Test
    void createsAndParsesAccessTokenWithUserSubjectAndRole() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserDetails user = userDetails(userId, UserRole.ADMIN);

        String token = tokenService.createAccessToken(user);

        ParsedAccessToken parsed = tokenService.parseAccessToken(token);
        assertThat(parsed.userId()).isEqualTo(userId);
        assertThat(parsed.role()).isEqualTo(UserRole.ADMIN.name());
    }

    @Test
    void createsAndParsesRefreshTokenWithSessionId() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        CustomUserDetails user = userDetails(userId, UserRole.USER);

        String token = tokenService.createRefreshToken(user, sessionId);

        ParsedRefreshToken parsed = tokenService.parseRefreshToken(token);
        assertThat(parsed.userId()).isEqualTo(userId);
        assertThat(parsed.sessionId()).isEqualTo(sessionId);
    }

    @Test
    void rejectsRefreshTokenWhenAccessTokenIsExpected() throws Exception {
        String refreshToken = tokenService.createRefreshToken(userDetails(UUID.randomUUID(), UserRole.USER), UUID.randomUUID());

        assertThatThrownBy(() -> tokenService.parseAccessToken(refreshToken))
                .isInstanceOf(AppException.class)
                .hasMessage("Invalid token type");
    }

    @Test
    void rejectsTamperedToken() throws Exception {
        String token = tokenService.createAccessToken(userDetails(UUID.randomUUID(), UserRole.USER));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> tokenService.parseAccessToken(tampered))
                .isInstanceOf(AppException.class)
                .hasMessage("Invalid or expired token");
    }

    private CustomUserDetails userDetails(UUID id, UserRole role) throws Exception {
        User user = new User();
        setId(user, id);
        user.setEmail("ada@example.com");
        user.setPassword("hash");
        user.setRole(role);
        return new CustomUserDetails(user);
    }

    private void setId(User user, UUID id) throws Exception {
        Field field = user.getClass().getSuperclass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
./gradlew test --tests com.giabao.linkflow.auth.service.TokenServiceTest
```

Expected: FAIL because token service classes do not exist.

- [ ] **Step 3: Create token DTO records and enum**

Create `src/main/java/com/giabao/linkflow/auth/service/TokenType.java`:

```java
package com.giabao.linkflow.auth.service;

public enum TokenType {
    ACCESS,
    REFRESH
}
```

Create `src/main/java/com/giabao/linkflow/auth/service/ParsedAccessToken.java`:

```java
package com.giabao.linkflow.auth.service;

import java.util.UUID;

public record ParsedAccessToken(UUID userId, String role) {
}
```

Create `src/main/java/com/giabao/linkflow/auth/service/ParsedRefreshToken.java`:

```java
package com.giabao.linkflow.auth.service;

import java.util.UUID;

public record ParsedRefreshToken(UUID userId, UUID sessionId) {
}
```

- [ ] **Step 4: Create `TokenService`**

Create `src/main/java/com/giabao/linkflow/auth/service/TokenService.java`:

```java
package com.giabao.linkflow.auth.service;

import com.giabao.linkflow.auth.config.JwtProperties;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.security.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class TokenService {
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ROLE_CLAIM = "role";
    private static final String SESSION_ID_CLAIM = "sid";
    private static final String INVALID_TOKEN_MESSAGE = "Invalid or expired token";
    private static final String INVALID_TOKEN_TYPE_MESSAGE = "Invalid token type";
    private static final String INVALID_TOKEN_SUBJECT_MESSAGE = "Invalid token subject";

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public TokenService(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = jwtProperties.accessTokenExpirationMs();
        this.refreshTokenExpirationMs = jwtProperties.refreshTokenExpirationMs();
    }

    public String createAccessToken(CustomUserDetails user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(accessTokenExpirationMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(TOKEN_TYPE_CLAIM, TokenType.ACCESS.name())
                .claim(ROLE_CLAIM, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String createRefreshToken(CustomUserDetails user, UUID sessionId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(TOKEN_TYPE_CLAIM, TokenType.REFRESH.name())
                .claim(SESSION_ID_CLAIM, sessionId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public ParsedAccessToken parseAccessToken(String token) {
        Claims claims = parseAndVerify(token);
        requireTokenType(claims, TokenType.ACCESS);
        return new ParsedAccessToken(parseUuid(claims.getSubject()), claims.get(ROLE_CLAIM, String.class));
    }

    public ParsedRefreshToken parseRefreshToken(String token) {
        Claims claims = parseAndVerify(token);
        requireTokenType(claims, TokenType.REFRESH);
        return new ParsedRefreshToken(
                parseUuid(claims.getSubject()),
                parseUuid(claims.get(SESSION_ID_CLAIM, String.class))
        );
    }

    private Claims parseAndVerify(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AppException(ErrorCode.UNAUTHORIZED, INVALID_TOKEN_MESSAGE);
        }
    }

    private void requireTokenType(Claims claims, TokenType expectedType) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expectedType.name().equals(tokenType)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, INVALID_TOKEN_TYPE_MESSAGE);
        }
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new AppException(ErrorCode.UNAUTHORIZED, INVALID_TOKEN_SUBJECT_MESSAGE);
        }
    }
}
```

- [ ] **Step 5: Run token tests**

Run:

```bash
./gradlew test --tests com.giabao.linkflow.auth.service.TokenServiceTest
```

Expected: PASS.

---

### Task 5: Redis Refresh Session Service

**Files:**
- Create: `src/main/java/com/giabao/linkflow/auth/service/RefreshSession.java`
- Create: `src/main/java/com/giabao/linkflow/auth/service/RefreshSessionService.java`
- Test: `src/test/java/com/giabao/linkflow/auth/service/RefreshSessionServiceTest.java`

- [ ] **Step 1: Write failing refresh session tests**

Create `src/test/java/com/giabao/linkflow/auth/service/RefreshSessionServiceTest.java`:

```java
package com.giabao.linkflow.auth.service;

import com.giabao.linkflow.auth.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshSessionServiceTest {

    @Mock
    private RedisTemplate<String, RefreshSession> redisTemplate;

    @Mock
    private ValueOperations<String, RefreshSession> valueOperations;

    @Test
    void savesHashInsteadOfRawRefreshToken() {
        RefreshSessionService service = service();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.save(sessionId, userId, "raw-refresh-token");

        ArgumentCaptor<RefreshSession> sessionCaptor = ArgumentCaptor.forClass(RefreshSession.class);
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq("auth:refresh-session:" + sessionId),
                sessionCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMillis(604800000))
        );
        assertThat(sessionCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(sessionCaptor.getValue().refreshTokenHash()).isNotEqualTo("raw-refresh-token");
        assertThat(sessionCaptor.getValue().refreshTokenHash()).isNotBlank();
    }

    @Test
    void validatesMatchingRefreshToken() {
        RefreshSessionService service = service();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RefreshSession session = new RefreshSession(userId, service.hash("raw-refresh-token"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:refresh-session:" + sessionId)).thenReturn(session);

        Optional<RefreshSession> result = service.findValidSession(sessionId, "raw-refresh-token");

        assertThat(result).contains(session);
    }

    @Test
    void returnsEmptyAndDeletesSessionForMismatch() {
        RefreshSessionService service = service();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RefreshSession session = new RefreshSession(userId, service.hash("expected-token"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:refresh-session:" + sessionId)).thenReturn(session);

        Optional<RefreshSession> result = service.findValidSession(sessionId, "reused-token");

        assertThat(result).isEmpty();
        verify(redisTemplate).delete("auth:refresh-session:" + sessionId);
    }

    private RefreshSessionService service() {
        return new RefreshSessionService(redisTemplate, new JwtProperties(
                "12345678901234567890123456789012",
                900000,
                604800000
        ));
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
./gradlew test --tests com.giabao.linkflow.auth.service.RefreshSessionServiceTest
```

Expected: FAIL because refresh session classes do not exist.

- [ ] **Step 3: Create refresh session record**

Create `src/main/java/com/giabao/linkflow/auth/service/RefreshSession.java`:

```java
package com.giabao.linkflow.auth.service;

import java.util.UUID;

public record RefreshSession(UUID userId, String refreshTokenHash) {
}
```

- [ ] **Step 4: Create refresh session service**

Create `src/main/java/com/giabao/linkflow/auth/service/RefreshSessionService.java`:

```java
package com.giabao.linkflow.auth.service;

import com.giabao.linkflow.auth.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshSessionService {
    private static final String KEY_PREFIX = "auth:refresh-session:";
    private static final String HASH_ALGORITHM = "SHA-256";

    private final RedisTemplate<String, RefreshSession> redisTemplate;
    private final JwtProperties jwtProperties;

    public void save(UUID sessionId, UUID userId, String refreshToken) {
        RefreshSession session = new RefreshSession(userId, hash(refreshToken));
        redisTemplate.opsForValue().set(key(sessionId), session, Duration.ofMillis(jwtProperties.refreshTokenExpirationMs()));
    }

    public Optional<RefreshSession> findValidSession(UUID sessionId, String refreshToken) {
        RefreshSession session = redisTemplate.opsForValue().get(key(sessionId));
        if (session == null) {
            return Optional.empty();
        }

        if (!MessageDigest.isEqual(
                session.refreshTokenHash().getBytes(StandardCharsets.UTF_8),
                hash(refreshToken).getBytes(StandardCharsets.UTF_8)
        )) {
            delete(sessionId);
            return Optional.empty();
        }

        return Optional.of(session);
    }

    public void delete(UUID sessionId) {
        redisTemplate.delete(key(sessionId));
    }

    String hash(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Refresh token hash algorithm is unavailable", exception);
        }
    }

    private String key(UUID sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
```

- [ ] **Step 5: Run refresh session tests**

Run:

```bash
./gradlew test --tests com.giabao.linkflow.auth.service.RefreshSessionServiceTest
```

Expected: PASS.

---

### Task 6: Refresh Cookie Service

**Files:**
- Create: `src/main/java/com/giabao/linkflow/auth/service/RefreshCookieService.java`
- Test: `src/test/java/com/giabao/linkflow/auth/service/RefreshCookieServiceTest.java`

- [ ] **Step 1: Write failing cookie service tests**

Create `src/test/java/com/giabao/linkflow/auth/service/RefreshCookieServiceTest.java`:

```java
package com.giabao.linkflow.auth.service;

import com.giabao.linkflow.auth.config.JwtProperties;
import com.giabao.linkflow.auth.config.RefreshCookieProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshCookieServiceTest {

    private final RefreshCookieService service = new RefreshCookieService(
            new RefreshCookieProperties("refresh_token", true, false, "Lax", "/api/v1/auth/refresh"),
            new JwtProperties("12345678901234567890123456789012", 900000, 604800000)
    );

    @Test
    void addsRefreshCookieWithConfiguredAttributes() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.addRefreshCookie(response, "refresh-token-value");

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains("refresh_token=refresh-token-value");
        assertThat(setCookie).contains("Path=/api/v1/auth/refresh");
        assertThat(setCookie).contains("Max-Age=604800");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=Lax");
    }

    @Test
    void clearsRefreshCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.clearRefreshCookie(response);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains("refresh_token=");
        assertThat(setCookie).contains("Max-Age=0");
        assertThat(setCookie).contains("Path=/api/v1/auth/refresh");
    }

    @Test
    void extractsRefreshCookieValue() {
        Cookie[] cookies = {new Cookie("refresh_token", "abc")};

        assertThat(service.extractRefreshToken(cookies)).contains("abc");
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
./gradlew test --tests com.giabao.linkflow.auth.service.RefreshCookieServiceTest
```

Expected: FAIL because `RefreshCookieService` does not exist.

- [ ] **Step 3: Create refresh cookie service**

Create `src/main/java/com/giabao/linkflow/auth/service/RefreshCookieService.java`:

```java
package com.giabao.linkflow.auth.service;

import com.giabao.linkflow.auth.config.JwtProperties;
import com.giabao.linkflow.auth.config.RefreshCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshCookieService {
    private final RefreshCookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    public void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = baseCookie(refreshToken)
                .maxAge(Duration.ofMillis(jwtProperties.refreshTokenExpirationMs()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public Optional<String> extractRefreshToken(Cookie[] cookies) {
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieProperties.name().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(cookieProperties.name(), value)
                .httpOnly(cookieProperties.httpOnly())
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(cookieProperties.path());
    }
}
```

- [ ] **Step 4: Run cookie tests**

Run:

```bash
./gradlew test --tests com.giabao.linkflow.auth.service.RefreshCookieServiceTest
```

Expected: PASS.

---

### Task 7: Auth Service and Controller

**Files:**
- Create: `src/main/java/com/giabao/linkflow/auth/dto/req/RegisterRequest.java`
- Create: `src/main/java/com/giabao/linkflow/auth/dto/req/LoginRequest.java`
- Create: `src/main/java/com/giabao/linkflow/auth/dto/res/TokenResponse.java`
- Create: `src/main/java/com/giabao/linkflow/auth/dto/res/AuthResponse.java`
- Create: `src/main/java/com/giabao/linkflow/auth/service/AuthService.java`
- Create: `src/main/java/com/giabao/linkflow/auth/controller/AuthController.java`
- Test: `src/test/java/com/giabao/linkflow/auth/service/AuthServiceTest.java`
- Test: `src/test/java/com/giabao/linkflow/auth/controller/AuthControllerTest.java`

- [ ] **Step 1: Write failing AuthService tests**

Create `src/test/java/com/giabao/linkflow/auth/service/AuthServiceTest.java` with focused tests for login, refresh mismatch, and logout. Use mocks for `AuthenticationManager`, `UserService`, `TokenService`, `RefreshSessionService`, and `RefreshCookieService`. The login success test must assert that `authenticationManager.authenticate(...)` is called and that `RefreshSessionService.save(...)` receives the generated session id and refresh token.

Use this exact login success test body:

```java
@Test
void loginAuthenticatesWithAuthenticationManagerAndCreatesRefreshSession() throws Exception {
    CustomUserDetails userDetails = userDetails(UUID.randomUUID(), UserRole.USER);
    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    LoginRequest request = new LoginRequest("ada@example.com", "secret-password");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
    when(tokenService.createAccessToken(userDetails)).thenReturn("access-token");
    when(tokenService.createRefreshToken(eq(userDetails), any(UUID.class))).thenReturn("refresh-token");

    AuthResponse response = authService.login(request, httpServletResponse);

    assertThat(response.accessToken()).isEqualTo("access-token");
    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(refreshSessionService).save(any(UUID.class), eq(userDetails.getId()), eq("refresh-token"));
    verify(refreshCookieService).addRefreshCookie(httpServletResponse, "refresh-token");
}
```

- [ ] **Step 2: Create DTOs**

Create `RegisterRequest`, `LoginRequest`, `TokenResponse`, and `AuthResponse`:

```java
package com.giabao.linkflow.auth.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 255) String fullName
) {
}
```

```java
package com.giabao.linkflow.auth.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
```

```java
package com.giabao.linkflow.auth.dto.res;

public record TokenResponse(String accessToken) {
}
```

```java
package com.giabao.linkflow.auth.dto.res;

public record AuthResponse(String accessToken) {
}
```

- [ ] **Step 3: Implement `AuthService`**

Create `src/main/java/com/giabao/linkflow/auth/service/AuthService.java` with methods:

```java
public UserResponse register(RegisterRequest request)
public AuthResponse login(LoginRequest request, HttpServletResponse response)
public TokenResponse refresh(Cookie[] cookies, HttpServletResponse response)
public void logout(Cookie[] cookies, HttpServletResponse response)
```

Implementation rules:

- `register` maps to `CreateUserRequest` with role `UserRole.USER` and delegates to `UserService.createUser`.
- `login` calls `AuthenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password))`.
- `login` creates `sessionId`, access token, refresh token, saves refresh session, and adds refresh cookie.
- `refresh` extracts cookie, parses refresh token, validates Redis session, rotates refresh token, and adds new cookie.
- `logout` extracts refresh cookie, parses it if possible, deletes Redis session, and always clears cookie.
- Missing/invalid refresh cookie throws `new AppException(ErrorCode.UNAUTHORIZED)`.

- [ ] **Step 4: Write AuthController tests**

Create `src/test/java/com/giabao/linkflow/auth/controller/AuthControllerTest.java` using `@WebMvcTest(AuthController.class)` and `@AutoConfigureMockMvc(addFilters = false)`. Cover:

- `POST /api/v1/auth/register` returns `201`, message `User registered successfully`, and `UserResponse`.
- invalid register returns `400` with validation envelope.
- `POST /api/v1/auth/login` returns access token and delegates to service.
- `POST /api/v1/auth/refresh` returns access token.
- `POST /api/v1/auth/logout` returns success.

- [ ] **Step 5: Implement `AuthController`**

Create `src/main/java/com/giabao/linkflow/auth/controller/AuthController.java`:

```java
package com.giabao.linkflow.auth.controller;

import com.giabao.linkflow.auth.dto.req.LoginRequest;
import com.giabao.linkflow.auth.dto.req.RegisterRequest;
import com.giabao.linkflow.auth.dto.res.AuthResponse;
import com.giabao.linkflow.auth.dto.res.TokenResponse;
import com.giabao.linkflow.auth.service.AuthService;
import com.giabao.linkflow.common.annotation.ApiMessage;
import com.giabao.linkflow.user.dto.res.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @ApiMessage("User registered successfully")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @ApiMessage("User logged in successfully")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(authService.login(request, response));
    }

    @PostMapping("/refresh")
    @ApiMessage("Access token refreshed successfully")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(authService.refresh(request.getCookies(), response));
    }

    @PostMapping("/logout")
    @ApiMessage("User logged out successfully")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request.getCookies(), response);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 6: Run auth tests**

Run:

```bash
./gradlew test --tests 'com.giabao.linkflow.auth.*'
```

Expected: PASS for auth service, controller, config, token, cookie, and refresh session tests.

---

### Task 8: JWT Filter and Security Config

**Files:**
- Create: `src/main/java/com/giabao/linkflow/security/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/giabao/linkflow/security/RestAuthenticationEntryPoint.java`
- Create: `src/main/java/com/giabao/linkflow/security/RestAccessDeniedHandler.java`
- Create: `src/main/java/com/giabao/linkflow/security/SecurityConfig.java`
- Test: `src/test/java/com/giabao/linkflow/security/JwtAuthenticationFilterTest.java`
- Test: `src/test/java/com/giabao/linkflow/security/SecurityConfigTest.java`

- [ ] **Step 1: Write failing JWT filter test**

Create `JwtAuthenticationFilterTest` that mocks `TokenService` and `CustomUserDetailsService`, sends a request with `Authorization: Bearer access-token`, and asserts `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` is the expected `CustomUserDetails` after the filter chain runs.

- [ ] **Step 2: Implement `JwtAuthenticationFilter`**

Create `src/main/java/com/giabao/linkflow/security/JwtAuthenticationFilter.java` using the code shape from the approved design:

```java
package com.giabao.linkflow.security;

import com.giabao.linkflow.auth.dto.ParsedAccessToken;
import com.giabao.linkflow.auth.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractBearerToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            ParsedAccessToken parsedToken = tokenService.parseAccessToken(token);
            CustomUserDetails userDetails = customUserDetailsService.loadUserById(parsedToken.userId());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
```

- [ ] **Step 3: Add REST security handlers**

Create `RestAuthenticationEntryPoint` and `RestAccessDeniedHandler` that set status `401` or `403`, content type JSON, and write `ApiResponse` with `ErrorCode.UNAUTHORIZED` or `ErrorCode.FORBIDDEN` through `ObjectMapper`.

- [ ] **Step 4: Implement `SecurityConfig`**

Create `src/main/java/com/giabao/linkflow/security/SecurityConfig.java`:

```java
package com.giabao.linkflow.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/*").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(daoAuthenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
```

- [ ] **Step 5: Run security tests**

Run:

```bash
./gradlew test --tests 'com.giabao.linkflow.security.*'
```

Expected: PASS.

---

### Task 9: Full Verification and Regression Pass

**Files:**
- Review all changed files.
- No new files unless a compile/test failure identifies a missing focused helper.

- [ ] **Step 1: Run full test suite**

Run:

```bash
./gradlew test
```

Expected: PASS.

- [ ] **Step 2: Run build**

Run:

```bash
./gradlew build
```

Expected: PASS.

- [ ] **Step 3: Inspect changed files**

Run:

```bash
git status --short
git diff --stat
git diff --check
```

Expected:

- `git diff --check` prints no whitespace errors.
- Changed files match the auth/security/config/test scope.
- Existing user changes in `AGENTS.md` and `docs/architecture.md` remain untouched unless the user separately asks to handle them.

- [ ] **Step 4: Manual API smoke test if Docker is available**

Run infrastructure:

```bash
docker compose up -d postgres redis
```

Run app:

```bash
./gradlew bootRun
```

In a second terminal, test register and login:

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","password":"secret-password","fullName":"Ada Lovelace"}'

curl -i -c /tmp/linkflow-cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","password":"secret-password"}'
```

Expected login response:

- HTTP 200.
- JSON envelope contains `data.accessToken`.
- Headers include `Set-Cookie: refresh_token=...; Path=/api/v1/auth/refresh; Max-Age=...; HttpOnly; SameSite=Lax`.

Test refresh:

```bash
curl -i -b /tmp/linkflow-cookies.txt -c /tmp/linkflow-cookies.txt \
  -X POST http://localhost:8080/api/v1/auth/refresh
```

Expected refresh response:

- HTTP 200.
- JSON envelope contains a new `data.accessToken`.
- Response sets a new `refresh_token` cookie.

Test logout:

```bash
curl -i -b /tmp/linkflow-cookies.txt -X POST http://localhost:8080/api/v1/auth/logout
```

Expected logout response:

- HTTP 200.
- Response clears `refresh_token` with `Max-Age=0`.

- [ ] **Step 5: Final report without commit**

Report:

- Changed files.
- Tests/build commands and exact pass/fail status.
- Manual smoke test status, or why it was skipped.
- Any remaining risks, especially cookie/CORS production config.

---

## Self-Review Notes

- Spec coverage: plan covers Spring Security login, JWT filter, JJWT verification, Redis refresh sessions, HttpOnly refresh cookie, rotation, current-session logout, admin-only `/users/**`, and tests.
- Placeholder scan: no `TBD`, `TODO`, or unspecified future implementation steps should remain in this plan.
- Type consistency: `AuthResponse`, `TokenResponse`, `ParsedAccessToken`, `ParsedRefreshToken`, `CustomUserDetails`, and service method names are defined before use or in the same task.
- User constraint: no commit steps are included because the user explicitly requested no autonomous commits.
