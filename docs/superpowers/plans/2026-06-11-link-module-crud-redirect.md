# Link Module (CRUD + Redirect) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let an authenticated user create/read/update/delete/list their own short links (with server-generated short codes) and let any visitor hit `GET /{shortCode}` to be 302-redirected to the original URL while the link's click count is incremented.

**Architecture:** Package-by-feature, mirroring the existing `user` module. A `link` package holds CRUD (controller → `LinkService` → `LinkRepository`, MapStruct mapper, JPA `Specification` filtering, a `ShortCodeGenerator`). A separate `redirect` package holds the public redirect endpoint and a thin `RedirectService`. Ownership is enforced in the service layer by resolving every single-link operation with `findByIdAndUserId`; the redirect path returns body-less `ResponseEntity<Void>` (302 or 404) so it bypasses the `ApiResponse` envelope.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Data JPA, MapStruct, Hibernate Validator, JUnit 5 + Mockito + AssertJ, Spring `@WebMvcTest`/MockMvc. Build: Gradle (`./gradlew`).

**Spec:** `docs/superpowers/specs/2026-06-11-link-module-crud-redirect-design.md`

---

## File Structure

**Create (main):**
- `src/main/java/com/giabao/linkflow/link/dto/request/CreateLinkRequest.java`
- `src/main/java/com/giabao/linkflow/link/dto/request/UpdateLinkRequest.java`
- `src/main/java/com/giabao/linkflow/link/dto/request/LinkFilterRequest.java`
- `src/main/java/com/giabao/linkflow/link/dto/response/LinkResponse.java`
- `src/main/java/com/giabao/linkflow/link/repository/LinkRepository.java`
- `src/main/java/com/giabao/linkflow/link/mapper/LinkMapper.java`
- `src/main/java/com/giabao/linkflow/link/service/ShortCodeGenerator.java`
- `src/main/java/com/giabao/linkflow/link/specification/LinkSpecifications.java`
- `src/main/java/com/giabao/linkflow/link/service/LinkService.java`
- `src/main/java/com/giabao/linkflow/link/service/impl/LinkServiceImpl.java`
- `src/main/java/com/giabao/linkflow/link/controller/LinkController.java`
- `src/main/java/com/giabao/linkflow/redirect/service/RedirectService.java`
- `src/main/java/com/giabao/linkflow/redirect/controller/RedirectController.java`

**Create (test):**
- `src/test/java/com/giabao/linkflow/link/service/ShortCodeGeneratorTest.java`
- `src/test/java/com/giabao/linkflow/link/service/LinkServiceImplTest.java`
- `src/test/java/com/giabao/linkflow/link/controller/LinkControllerTest.java`
- `src/test/java/com/giabao/linkflow/redirect/service/RedirectServiceTest.java`
- `src/test/java/com/giabao/linkflow/redirect/controller/RedirectControllerTest.java`

**Modify:**
- `src/main/java/com/giabao/linkflow/user/controller/UserController.java` (`/users` → `/api/v1/users`)
- `src/main/java/com/giabao/linkflow/security/SecurityConfig.java:44` (`/users/**` → `/api/v1/users/**`)
- `src/test/java/com/giabao/linkflow/user/controller/UserControllerTest.java` (request paths)

**Unchanged (already exist):** `link/entity/Link.java`, `links` table in `V1__init_schema.sql` (no new migration this slice).

---

## Task 1: Normalize User API prefix to `/api/v1/users`

**Files:**
- Modify: `src/test/java/com/giabao/linkflow/user/controller/UserControllerTest.java`
- Modify: `src/main/java/com/giabao/linkflow/user/controller/UserController.java:27`
- Modify: `src/main/java/com/giabao/linkflow/security/SecurityConfig.java:44`

- [ ] **Step 1: Update the controller test paths to the new prefix**

In `UserControllerTest.java`, replace every URL literal that starts with `/users` with `/api/v1/users`. The occurrences are in these calls:

```java
mockMvc.perform(get("/api/v1/users")
                .param("page", "1")
                .param("size", "5")
                .param("email", "ada")
                .param("role", "ADMIN"))
```
```java
mockMvc.perform(get("/api/v1/users/{id}", userId))
```
```java
mockMvc.perform(post("/api/v1/users")
```
```java
mockMvc.perform(get("/api/v1/users/not-a-uuid"))
```
```java
mockMvc.perform(get("/api/v1/users")
                .param("role", "INVALID"))
```
```java
mockMvc.perform(put("/api/v1/users/{id}", userId)
```
```java
mockMvc.perform(delete("/api/v1/users/{id}", userId))
```

(There are two `post("/users")` calls, two `put("/users/{id}", ...)` calls, and several `get`/`delete` — update every one.)

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests 'com.giabao.linkflow.user.controller.UserControllerTest'`
Expected: FAIL — requests now hit `/api/v1/users` but the controller still maps `/users`, so MockMvc returns 404 and assertions like `status().isOk()` fail.

- [ ] **Step 3: Move the controller mapping**

In `UserController.java`, change the class-level mapping:

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
```

- [ ] **Step 4: Update the security matcher**

In `SecurityConfig.java`, change line 44:

```java
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew test --tests 'com.giabao.linkflow.user.controller.UserControllerTest'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/giabao/linkflow/user/controller/UserController.java \
        src/main/java/com/giabao/linkflow/security/SecurityConfig.java \
        src/test/java/com/giabao/linkflow/user/controller/UserControllerTest.java
git commit -m "refactor(user): move User API under /api/v1 prefix"
```

---

## Task 2: Link DTOs (requests, filter, response)

**Files:**
- Create: `src/main/java/com/giabao/linkflow/link/dto/request/CreateLinkRequest.java`
- Create: `src/main/java/com/giabao/linkflow/link/dto/request/UpdateLinkRequest.java`
- Create: `src/main/java/com/giabao/linkflow/link/dto/request/LinkFilterRequest.java`
- Create: `src/main/java/com/giabao/linkflow/link/dto/response/LinkResponse.java`

These are data holders with no behavior; they are exercised by the controller/service tests in later tasks, so there is no separate test here. The verification step is compilation.

- [ ] **Step 1: Create `CreateLinkRequest`**

```java
package com.giabao.linkflow.link.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

public record CreateLinkRequest(
        @NotBlank
        @URL
        String originalUrl,

        @Size(max = 255)
        String title,

        @Future
        Instant expiresAt
) {
}
```

- [ ] **Step 2: Create `UpdateLinkRequest`** (same shape; full-replacement PUT semantics)

```java
package com.giabao.linkflow.link.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

public record UpdateLinkRequest(
        @NotBlank
        @URL
        String originalUrl,

        @Size(max = 255)
        String title,

        @Future
        Instant expiresAt
) {
}
```

- [ ] **Step 3: Create `LinkFilterRequest`** (extends `PageableRequest`)

```java
package com.giabao.linkflow.link.dto.request;

import com.giabao.linkflow.common.dto.PageableRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class LinkFilterRequest extends PageableRequest {
    public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "updatedAt",
            "clickCount",
            "title"
    );
    public static final String DEFAULT_SORT_BY = "createdAt";

    private String title;
    private Boolean active;
}
```

- [ ] **Step 4: Create `LinkResponse`**

```java
package com.giabao.linkflow.link.dto.response;

import java.time.Instant;
import java.util.UUID;

public record LinkResponse(
        UUID id,
        String originalUrl,
        String shortCode,
        String title,
        Long clickCount,
        Boolean active,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/giabao/linkflow/link/dto
git commit -m "feat(link): add request/response DTOs"
```

---

## Task 3: LinkRepository

**Files:**
- Create: `src/main/java/com/giabao/linkflow/link/repository/LinkRepository.java`

Derived query methods plus one `@Modifying` atomic click increment. Verified at compile time here and exercised by the service/redirect unit tests (which verify `incrementClickCount` is invoked); the JPQL is standard.

- [ ] **Step 1: Create the repository**

```java
package com.giabao.linkflow.link.repository;

import com.giabao.linkflow.link.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkRepository extends JpaRepository<Link, UUID>, JpaSpecificationExecutor<Link> {

    Optional<Link> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Optional<Link> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("update Link l set l.clickCount = l.clickCount + 1 where l.id = :id")
    int incrementClickCount(@Param("id") UUID id);
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/giabao/linkflow/link/repository/LinkRepository.java
git commit -m "feat(link): add LinkRepository with lookup and atomic click increment"
```

---

## Task 4: LinkMapper

**Files:**
- Create: `src/main/java/com/giabao/linkflow/link/mapper/LinkMapper.java`

- [ ] **Step 1: Create the MapStruct mapper**

Server-managed fields (`user`, `shortCode`, `clickCount`, `active`) are ignored on the inbound mappings; they are set by the service or default on the entity.

```java
package com.giabao.linkflow.link.mapper;

import com.giabao.linkflow.link.dto.request.CreateLinkRequest;
import com.giabao.linkflow.link.dto.request.UpdateLinkRequest;
import com.giabao.linkflow.link.dto.response.LinkResponse;
import com.giabao.linkflow.link.entity.Link;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LinkMapper {

    LinkResponse toResponse(Link link);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "shortCode", ignore = true)
    @Mapping(target = "clickCount", ignore = true)
    @Mapping(target = "active", ignore = true)
    Link toEntity(CreateLinkRequest request);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "shortCode", ignore = true)
    @Mapping(target = "clickCount", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(UpdateLinkRequest request, @MappingTarget Link link);
}
```

- [ ] **Step 2: Verify the mapper implementation generates and compiles**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL (MapStruct generates `LinkMapperImpl`).

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/giabao/linkflow/link/mapper/LinkMapper.java
git commit -m "feat(link): add LinkMapper"
```

---

## Task 5: ShortCodeGenerator (TDD)

**Files:**
- Create: `src/test/java/com/giabao/linkflow/link/service/ShortCodeGeneratorTest.java`
- Create: `src/main/java/com/giabao/linkflow/link/service/ShortCodeGenerator.java`

- [ ] **Step 1: Write the failing test**

```java
package com.giabao.linkflow.link.service;

import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.link.repository.LinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortCodeGeneratorTest {

    @Mock
    private LinkRepository linkRepository;

    @InjectMocks
    private ShortCodeGenerator shortCodeGenerator;

    @Test
    void generatesSevenCharBase62CodeWhenNoCollision() {
        when(linkRepository.existsByShortCode(anyString())).thenReturn(false);

        String code = shortCodeGenerator.generate();

        assertThat(code).hasSize(7).matches("[A-Za-z0-9]{7}");
        verify(linkRepository).existsByShortCode(code);
    }

    @Test
    void retriesOnCollisionThenReturnsUniqueCode() {
        when(linkRepository.existsByShortCode(anyString())).thenReturn(true, true, false);

        String code = shortCodeGenerator.generate();

        assertThat(code).hasSize(7);
        verify(linkRepository, times(3)).existsByShortCode(anyString());
    }

    @Test
    void throwsAfterMaxAttempts() {
        when(linkRepository.existsByShortCode(anyString())).thenReturn(true);

        assertThatThrownBy(() -> shortCodeGenerator.generate())
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR)
                );

        verify(linkRepository, times(5)).existsByShortCode(anyString());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests 'com.giabao.linkflow.link.service.ShortCodeGeneratorTest'`
Expected: FAIL — compilation error, `ShortCodeGenerator` does not exist.

- [ ] **Step 3: Write the implementation**

```java
package com.giabao.linkflow.link.service;

import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.link.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class ShortCodeGenerator {
    private static final String ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 5;
    private static final String GENERATION_FAILED_MESSAGE = "Could not generate a unique short code";

    private final LinkRepository linkRepository;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (!linkRepository.existsByShortCode(code)) {
                return code;
            }
        }

        throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, GENERATION_FAILED_MESSAGE);
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }

        return builder.toString();
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew test --tests 'com.giabao.linkflow.link.service.ShortCodeGeneratorTest'`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/giabao/linkflow/link/service/ShortCodeGenerator.java \
        src/test/java/com/giabao/linkflow/link/service/ShortCodeGeneratorTest.java
git commit -m "feat(link): add Base62 ShortCodeGenerator with collision retry"
```

---

## Task 6: LinkSpecifications

**Files:**
- Create: `src/main/java/com/giabao/linkflow/link/specification/LinkSpecifications.java`

Always scopes by owner; `title` and `active` are optional filters. The owner predicate navigates the `user` relation (`root.get("user").get("id")`), which `CommonSpecifications.equal` (single-field) cannot express, so it is written inline.

- [ ] **Step 1: Create the specifications**

```java
package com.giabao.linkflow.link.specification;

import com.giabao.linkflow.common.specification.CommonSpecifications;
import com.giabao.linkflow.link.dto.request.LinkFilterRequest;
import com.giabao.linkflow.link.entity.Link;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class LinkSpecifications {
    private LinkSpecifications() {
    }

    public static Specification<Link> byFilter(UUID ownerId, LinkFilterRequest filter) {
        return CommonSpecifications.allOf(
                ownedBy(ownerId),
                CommonSpecifications.containsIgnoreCase("title", filter.getTitle()),
                CommonSpecifications.equal("active", filter.getActive())
        );
    }

    private static Specification<Link> ownedBy(UUID ownerId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("user").get("id"), ownerId);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/giabao/linkflow/link/specification/LinkSpecifications.java
git commit -m "feat(link): add owner-scoped LinkSpecifications"
```

---

## Task 7: LinkService + LinkServiceImpl (TDD)

**Files:**
- Create: `src/main/java/com/giabao/linkflow/link/service/LinkService.java`
- Create: `src/test/java/com/giabao/linkflow/link/service/LinkServiceImplTest.java`
- Create: `src/main/java/com/giabao/linkflow/link/service/impl/LinkServiceImpl.java`

- [ ] **Step 1: Create the service interface**

```java
package com.giabao.linkflow.link.service;

import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.link.dto.request.CreateLinkRequest;
import com.giabao.linkflow.link.dto.request.LinkFilterRequest;
import com.giabao.linkflow.link.dto.request.UpdateLinkRequest;
import com.giabao.linkflow.link.dto.response.LinkResponse;

import java.util.UUID;

public interface LinkService {
    PageResponse<LinkResponse> getMyLinks(LinkFilterRequest request);

    LinkResponse getMyLink(UUID id);

    LinkResponse createLink(CreateLinkRequest request);

    LinkResponse updateLink(UUID id, UpdateLinkRequest request);

    LinkResponse activateLink(UUID id);

    LinkResponse deactivateLink(UUID id);

    void deleteLink(UUID id);
}
```

- [ ] **Step 2: Write the failing test**

```java
package com.giabao.linkflow.link.service;

import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.link.dto.request.CreateLinkRequest;
import com.giabao.linkflow.link.dto.request.LinkFilterRequest;
import com.giabao.linkflow.link.dto.request.UpdateLinkRequest;
import com.giabao.linkflow.link.dto.response.LinkResponse;
import com.giabao.linkflow.link.entity.Link;
import com.giabao.linkflow.link.mapper.LinkMapper;
import com.giabao.linkflow.link.repository.LinkRepository;
import com.giabao.linkflow.link.service.impl.LinkServiceImpl;
import com.giabao.linkflow.security.CurrentUserService;
import com.giabao.linkflow.security.CustomUserDetails;
import com.giabao.linkflow.user.entity.User;
import com.giabao.linkflow.user.entity.UserRole;
import com.giabao.linkflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkServiceImplTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private LinkMapper linkMapper;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LinkServiceImpl linkService;

    private final UUID currentUserId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        // lenient because not every test reads the current user
        Mockito.lenient().when(currentUserService.getCurrentUser())
                .thenReturn(new CustomUserDetails(currentUserId, UserRole.USER));
    }

    private Specification<Link> anySpecification() {
        return any();
    }

    @Test
    void createLinkAssignsShortCodeAndOwner() {
        CreateLinkRequest request = new CreateLinkRequest("https://example.com", "My link", null);
        Link mappedLink = new Link();
        User ownerRef = new User();
        LinkResponse response = new LinkResponse(
                UUID.randomUUID(), "https://example.com", "aB92xK7", "My link",
                0L, true, null, Instant.now(), Instant.now());

        when(linkMapper.toEntity(request)).thenReturn(mappedLink);
        when(shortCodeGenerator.generate()).thenReturn("aB92xK7");
        when(userRepository.getReferenceById(currentUserId)).thenReturn(ownerRef);
        when(linkRepository.save(mappedLink)).thenReturn(mappedLink);
        when(linkMapper.toResponse(mappedLink)).thenReturn(response);

        LinkResponse actual = linkService.createLink(request);

        assertThat(actual).isSameAs(response);
        ArgumentCaptor<Link> linkCaptor = ArgumentCaptor.forClass(Link.class);
        verify(linkRepository).save(linkCaptor.capture());
        Link saved = linkCaptor.getValue();
        assertThat(saved.getShortCode()).isEqualTo("aB92xK7");
        assertThat(saved.getUser()).isSameAs(ownerRef);
    }

    @Test
    void getMyLinkReturnsOwnedLink() {
        UUID linkId = UUID.randomUUID();
        Link link = new Link();
        LinkResponse response = new LinkResponse(
                linkId, "https://example.com", "aB92xK7", "My link",
                3L, true, null, Instant.now(), Instant.now());

        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.of(link));
        when(linkMapper.toResponse(link)).thenReturn(response);

        assertThat(linkService.getMyLink(linkId)).isSameAs(response);
    }

    @Test
    void getMyLinkThrowsNotFoundWhenNotOwned() {
        UUID linkId = UUID.randomUUID();
        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linkService.getMyLink(linkId))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND)
                )
                .hasMessage("Link not found");
    }

    @Test
    void updateLinkUpdatesOwnedLink() {
        UUID linkId = UUID.randomUUID();
        Link link = new Link();
        UpdateLinkRequest request = new UpdateLinkRequest("https://new.example.com", "New title", null);
        LinkResponse response = new LinkResponse(
                linkId, "https://new.example.com", "aB92xK7", "New title",
                0L, true, null, Instant.now(), Instant.now());

        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.of(link));
        when(linkRepository.save(link)).thenReturn(link);
        when(linkMapper.toResponse(link)).thenReturn(response);

        assertThat(linkService.updateLink(linkId, request)).isSameAs(response);
        verify(linkMapper).updateEntity(request, link);
    }

    @Test
    void activateLinkSetsActiveTrue() {
        UUID linkId = UUID.randomUUID();
        Link link = new Link();
        link.setActive(false);

        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.of(link));
        when(linkRepository.save(link)).thenReturn(link);
        when(linkMapper.toResponse(link)).thenReturn(
                new LinkResponse(linkId, "https://example.com", "aB92xK7", null,
                        0L, true, null, Instant.now(), Instant.now()));

        linkService.activateLink(linkId);

        assertThat(link.getActive()).isTrue();
        verify(linkRepository).save(link);
    }

    @Test
    void deactivateLinkSetsActiveFalse() {
        UUID linkId = UUID.randomUUID();
        Link link = new Link();
        link.setActive(true);

        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.of(link));
        when(linkRepository.save(link)).thenReturn(link);
        when(linkMapper.toResponse(link)).thenReturn(
                new LinkResponse(linkId, "https://example.com", "aB92xK7", null,
                        0L, false, null, Instant.now(), Instant.now()));

        linkService.deactivateLink(linkId);

        assertThat(link.getActive()).isFalse();
        verify(linkRepository).save(link);
    }

    @Test
    void deleteLinkRemovesOwnedLink() {
        UUID linkId = UUID.randomUUID();
        Link link = new Link();
        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.of(link));

        linkService.deleteLink(linkId);

        verify(linkRepository).delete(link);
    }

    @Test
    void deleteLinkThrowsNotFoundWhenNotOwned() {
        UUID linkId = UUID.randomUUID();
        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linkService.deleteLink(linkId))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND)
                );

        verify(linkRepository, never()).delete(any());
    }

    @Test
    void getMyLinksUsesSpecificationAndPageable() {
        LinkFilterRequest request = new LinkFilterRequest();
        request.setPage(1);
        request.setSize(10);
        request.setSortBy("clickCount");
        request.setSortDirection("desc");
        request.setTitle("promo");
        request.setActive(true);

        Link link = new Link();
        LinkResponse response = new LinkResponse(
                UUID.randomUUID(), "https://example.com", "aB92xK7", "promo",
                9L, true, null, Instant.now(), Instant.now());

        when(linkRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(link),
                        request.toPageable(LinkFilterRequest.ALLOWED_SORT_FIELDS, LinkFilterRequest.DEFAULT_SORT_BY),
                        21));
        when(linkMapper.toResponse(link)).thenReturn(response);

        PageResponse<LinkResponse> actual = linkService.getMyLinks(request);

        assertThat(actual.content()).containsExactly(response);
        assertThat(actual.totalElements()).isEqualTo(21);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(linkRepository).findAll(anySpecification(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "clickCount"));
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew test --tests 'com.giabao.linkflow.link.service.LinkServiceImplTest'`
Expected: FAIL — compilation error, `LinkServiceImpl` does not exist.

- [ ] **Step 4: Write the implementation**

```java
package com.giabao.linkflow.link.service.impl;

import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.link.dto.request.CreateLinkRequest;
import com.giabao.linkflow.link.dto.request.LinkFilterRequest;
import com.giabao.linkflow.link.dto.request.UpdateLinkRequest;
import com.giabao.linkflow.link.dto.response.LinkResponse;
import com.giabao.linkflow.link.entity.Link;
import com.giabao.linkflow.link.mapper.LinkMapper;
import com.giabao.linkflow.link.repository.LinkRepository;
import com.giabao.linkflow.link.service.LinkService;
import com.giabao.linkflow.link.service.ShortCodeGenerator;
import com.giabao.linkflow.link.specification.LinkSpecifications;
import com.giabao.linkflow.security.CurrentUserService;
import com.giabao.linkflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LinkServiceImpl implements LinkService {
    private static final String LINK_NOT_FOUND_MESSAGE = "Link not found";

    private final LinkRepository linkRepository;
    private final LinkMapper linkMapper;
    private final ShortCodeGenerator shortCodeGenerator;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LinkResponse> getMyLinks(LinkFilterRequest request) {
        Page<LinkResponse> links = linkRepository
                .findAll(
                        LinkSpecifications.byFilter(currentUserId(), request),
                        request.toPageable(LinkFilterRequest.ALLOWED_SORT_FIELDS, LinkFilterRequest.DEFAULT_SORT_BY)
                )
                .map(linkMapper::toResponse);

        return PageResponse.from(links);
    }

    @Override
    @Transactional(readOnly = true)
    public LinkResponse getMyLink(UUID id) {
        return linkMapper.toResponse(findOwnedLink(id));
    }

    @Override
    @Transactional
    public LinkResponse createLink(CreateLinkRequest request) {
        Link link = linkMapper.toEntity(request);
        link.setUser(userRepository.getReferenceById(currentUserId()));
        link.setShortCode(shortCodeGenerator.generate());

        return linkMapper.toResponse(linkRepository.save(link));
    }

    @Override
    @Transactional
    public LinkResponse updateLink(UUID id, UpdateLinkRequest request) {
        Link link = findOwnedLink(id);
        linkMapper.updateEntity(request, link);

        return linkMapper.toResponse(linkRepository.save(link));
    }

    @Override
    @Transactional
    public LinkResponse activateLink(UUID id) {
        return setActive(id, true);
    }

    @Override
    @Transactional
    public LinkResponse deactivateLink(UUID id) {
        return setActive(id, false);
    }

    @Override
    @Transactional
    public void deleteLink(UUID id) {
        linkRepository.delete(findOwnedLink(id));
    }

    private LinkResponse setActive(UUID id, boolean active) {
        Link link = findOwnedLink(id);
        link.setActive(active);

        return linkMapper.toResponse(linkRepository.save(link));
    }

    private Link findOwnedLink(UUID id) {
        return linkRepository.findByIdAndUserId(id, currentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, LINK_NOT_FOUND_MESSAGE));
    }

    private UUID currentUserId() {
        return currentUserService.getCurrentUser().getId();
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew test --tests 'com.giabao.linkflow.link.service.LinkServiceImplTest'`
Expected: PASS (9 tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/giabao/linkflow/link/service/LinkService.java \
        src/main/java/com/giabao/linkflow/link/service/impl/LinkServiceImpl.java \
        src/test/java/com/giabao/linkflow/link/service/LinkServiceImplTest.java
git commit -m "feat(link): add LinkService with owner-scoped CRUD"
```

---

## Task 8: LinkController (TDD)

**Files:**
- Create: `src/test/java/com/giabao/linkflow/link/controller/LinkControllerTest.java`
- Create: `src/main/java/com/giabao/linkflow/link/controller/LinkController.java`

`@WebMvcTest` loads `SecurityConfig`, so the same security collaborators the `UserControllerTest` mocks (`TokenService`, `RefreshSessionService`, `CustomUserDetailsService`) must be provided here too; `addFilters = false` bypasses the filter chain.

- [ ] **Step 1: Write the failing test**

```java
package com.giabao.linkflow.link.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giabao.linkflow.auth.service.RefreshSessionService;
import com.giabao.linkflow.auth.service.TokenService;
import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.link.dto.request.CreateLinkRequest;
import com.giabao.linkflow.link.dto.request.UpdateLinkRequest;
import com.giabao.linkflow.link.dto.response.LinkResponse;
import com.giabao.linkflow.link.service.LinkService;
import com.giabao.linkflow.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LinkController.class)
@AutoConfigureMockMvc(addFilters = false)
class LinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LinkService linkService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private RefreshSessionService refreshSessionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private LinkResponse sampleLink(UUID id) {
        return new LinkResponse(id, "https://example.com", "aB92xK7", "My link",
                0L, true, null, Instant.now(), Instant.now());
    }

    @Test
    void getMyLinksReturnsPagedLinks() throws Exception {
        UUID id = UUID.randomUUID();
        when(linkService.getMyLinks(any())).thenReturn(new PageResponse<>(
                1, 20, 1, 1, List.of(sampleLink(id))));

        mockMvc.perform(get("/api/v1/links").param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Links retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data.content[0].shortCode").value("aB92xK7"));
    }

    @Test
    void getLinkByIdReturnsLink() throws Exception {
        UUID id = UUID.randomUUID();
        when(linkService.getMyLink(id)).thenReturn(sampleLink(id));

        mockMvc.perform(get("/api/v1/links/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Link retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    void createLinkReturnsCreated() throws Exception {
        UUID id = UUID.randomUUID();
        CreateLinkRequest request = new CreateLinkRequest("https://example.com", "My link", null);
        when(linkService.createLink(any(CreateLinkRequest.class))).thenReturn(sampleLink(id));

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Link created successfully"))
                .andExpect(jsonPath("$.data.id").value(id.toString()));

        verify(linkService).createLink(any(CreateLinkRequest.class));
    }

    @Test
    void createLinkRejectsBlankUrl() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest("", "My link", null);

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.originalUrl").exists());

        verifyNoInteractions(linkService);
    }

    @Test
    void createLinkRejectsInvalidUrl() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest("not a url", "My link", null);

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.originalUrl").exists());

        verifyNoInteractions(linkService);
    }

    @Test
    void createLinkRejectsPastExpiry() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest(
                "https://example.com", "My link", Instant.now().minus(1, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.expiresAt").exists());

        verifyNoInteractions(linkService);
    }

    @Test
    void updateLinkReturnsUpdated() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateLinkRequest request = new UpdateLinkRequest("https://new.example.com", "New title", null);
        when(linkService.updateLink(any(UUID.class), any(UpdateLinkRequest.class))).thenReturn(sampleLink(id));

        mockMvc.perform(put("/api/v1/links/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Link updated successfully"));
    }

    @Test
    void activateLinkReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(linkService.activateLink(id)).thenReturn(sampleLink(id));

        mockMvc.perform(patch("/api/v1/links/{id}/activate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Link activated successfully"));
    }

    @Test
    void deactivateLinkReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(linkService.deactivateLink(id)).thenReturn(sampleLink(id));

        mockMvc.perform(patch("/api/v1/links/{id}/deactivate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Link deactivated successfully"));
    }

    @Test
    void deleteLinkReturnsSuccessEnvelope() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/links/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Link deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(linkService).deleteLink(id);
    }

    @Test
    void missingLinkReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(linkService.getMyLink(id)).thenThrow(new AppException(ErrorCode.NOT_FOUND, "Link not found"));

        mockMvc.perform(get("/api/v1/links/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Link not found"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests 'com.giabao.linkflow.link.controller.LinkControllerTest'`
Expected: FAIL — compilation error, `LinkController` does not exist.

- [ ] **Step 3: Write the controller**

```java
package com.giabao.linkflow.link.controller;

import com.giabao.linkflow.common.annotation.ApiMessage;
import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.link.dto.request.CreateLinkRequest;
import com.giabao.linkflow.link.dto.request.LinkFilterRequest;
import com.giabao.linkflow.link.dto.request.UpdateLinkRequest;
import com.giabao.linkflow.link.dto.response.LinkResponse;
import com.giabao.linkflow.link.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @GetMapping
    @ApiMessage("Links retrieved successfully")
    public ResponseEntity<PageResponse<LinkResponse>> getMyLinks(@ModelAttribute LinkFilterRequest request) {
        return ResponseEntity.ok(linkService.getMyLinks(request));
    }

    @GetMapping("/{id}")
    @ApiMessage("Link retrieved successfully")
    public ResponseEntity<LinkResponse> getMyLink(@PathVariable UUID id) {
        return ResponseEntity.ok(linkService.getMyLink(id));
    }

    @PostMapping
    @ApiMessage("Link created successfully")
    public ResponseEntity<LinkResponse> createLink(@Valid @RequestBody CreateLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(linkService.createLink(request));
    }

    @PutMapping("/{id}")
    @ApiMessage("Link updated successfully")
    public ResponseEntity<LinkResponse> updateLink(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLinkRequest request
    ) {
        return ResponseEntity.ok(linkService.updateLink(id, request));
    }

    @PatchMapping("/{id}/activate")
    @ApiMessage("Link activated successfully")
    public ResponseEntity<LinkResponse> activateLink(@PathVariable UUID id) {
        return ResponseEntity.ok(linkService.activateLink(id));
    }

    @PatchMapping("/{id}/deactivate")
    @ApiMessage("Link deactivated successfully")
    public ResponseEntity<LinkResponse> deactivateLink(@PathVariable UUID id) {
        return ResponseEntity.ok(linkService.deactivateLink(id));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Link deleted successfully")
    public ResponseEntity<Void> deleteLink(@PathVariable UUID id) {
        linkService.deleteLink(id);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew test --tests 'com.giabao.linkflow.link.controller.LinkControllerTest'`
Expected: PASS (11 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/giabao/linkflow/link/controller/LinkController.java \
        src/test/java/com/giabao/linkflow/link/controller/LinkControllerTest.java
git commit -m "feat(link): add LinkController for owner-scoped CRUD"
```

---

## Task 9: RedirectService (TDD)

**Files:**
- Create: `src/test/java/com/giabao/linkflow/redirect/service/RedirectServiceTest.java`
- Create: `src/main/java/com/giabao/linkflow/redirect/service/RedirectService.java`

Returns `Optional<String>` (the original URL) only when the link exists, is active, and is not expired — and increments the click count exactly in that case. The "active link" test uses a Mockito spy to control the entity's generated `id` (set on `BaseEntity` by JPA, not via a setter).

- [ ] **Step 1: Write the failing test**

```java
package com.giabao.linkflow.redirect.service;

import com.giabao.linkflow.link.entity.Link;
import com.giabao.linkflow.link.repository.LinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @InjectMocks
    private RedirectService redirectService;

    private Link link(boolean active, Instant expiresAt) {
        Link link = new Link();
        link.setOriginalUrl("https://example.com");
        link.setActive(active);
        link.setExpiresAt(expiresAt);
        return link;
    }

    @Test
    void resolvesActiveLinkAndIncrementsClick() {
        UUID id = UUID.randomUUID();
        Link spied = Mockito.spy(link(true, null));
        when(spied.getId()).thenReturn(id);
        when(linkRepository.findByShortCode("aB92xK7")).thenReturn(Optional.of(spied));

        Optional<String> url = redirectService.resolveActiveUrl("aB92xK7");

        assertThat(url).contains("https://example.com");
        verify(linkRepository).incrementClickCount(id);
    }

    @Test
    void returnsEmptyWhenMissing() {
        when(linkRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThat(redirectService.resolveActiveUrl("missing")).isEmpty();
        verify(linkRepository, never()).incrementClickCount(any());
    }

    @Test
    void returnsEmptyWhenInactive() {
        when(linkRepository.findByShortCode("aB92xK7")).thenReturn(Optional.of(link(false, null)));

        assertThat(redirectService.resolveActiveUrl("aB92xK7")).isEmpty();
        verify(linkRepository, never()).incrementClickCount(any());
    }

    @Test
    void returnsEmptyWhenExpired() {
        Link expired = link(true, Instant.now().minus(1, ChronoUnit.HOURS));
        when(linkRepository.findByShortCode("aB92xK7")).thenReturn(Optional.of(expired));

        assertThat(redirectService.resolveActiveUrl("aB92xK7")).isEmpty();
        verify(linkRepository, never()).incrementClickCount(any());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests 'com.giabao.linkflow.redirect.service.RedirectServiceTest'`
Expected: FAIL — compilation error, `RedirectService` does not exist.

- [ ] **Step 3: Write the implementation**

```java
package com.giabao.linkflow.redirect.service;

import com.giabao.linkflow.link.entity.Link;
import com.giabao.linkflow.link.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedirectService {

    private final LinkRepository linkRepository;

    @Transactional
    public Optional<String> resolveActiveUrl(String shortCode) {
        return linkRepository.findByShortCode(shortCode)
                .filter(this::isServable)
                .map(link -> {
                    linkRepository.incrementClickCount(link.getId());
                    return link.getOriginalUrl();
                });
    }

    private boolean isServable(Link link) {
        if (!Boolean.TRUE.equals(link.getActive())) {
            return false;
        }

        Instant expiresAt = link.getExpiresAt();
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew test --tests 'com.giabao.linkflow.redirect.service.RedirectServiceTest'`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/giabao/linkflow/redirect/service/RedirectService.java \
        src/test/java/com/giabao/linkflow/redirect/service/RedirectServiceTest.java
git commit -m "feat(redirect): add RedirectService resolving active links and counting clicks"
```

---

## Task 10: RedirectController (TDD)

**Files:**
- Create: `src/test/java/com/giabao/linkflow/redirect/controller/RedirectControllerTest.java`
- Create: `src/main/java/com/giabao/linkflow/redirect/controller/RedirectController.java`

The handler returns a body-less `ResponseEntity<Void>` (302 with `Location`, or 404), which bypasses the `ApiResponse` envelope. The path variable is regex-constrained to `[A-Za-z0-9]{6,32}` so it neither matches `/api/...` routes nor static paths, and aligns with the `SecurityConfig` permit-all regex.

- [ ] **Step 1: Write the failing test**

```java
package com.giabao.linkflow.redirect.controller;

import com.giabao.linkflow.auth.service.RefreshSessionService;
import com.giabao.linkflow.auth.service.TokenService;
import com.giabao.linkflow.redirect.service.RedirectService;
import com.giabao.linkflow.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RedirectController.class)
@AutoConfigureMockMvc(addFilters = false)
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RedirectService redirectService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private RefreshSessionService refreshSessionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void redirectsWithFoundAndLocationHeaderAndNoEnvelope() throws Exception {
        when(redirectService.resolveActiveUrl("aB92xK7")).thenReturn(Optional.of("https://example.com"));

        mockMvc.perform(get("/aB92xK7"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"))
                .andExpect(content().string(""));
    }

    @Test
    void returnsPlainNotFoundWhenUnresolved() throws Exception {
        when(redirectService.resolveActiveUrl("aB92xK7")).thenReturn(Optional.empty());

        mockMvc.perform(get("/aB92xK7"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests 'com.giabao.linkflow.redirect.controller.RedirectControllerTest'`
Expected: FAIL — compilation error, `RedirectController` does not exist.

- [ ] **Step 3: Write the controller**

```java
package com.giabao.linkflow.redirect.controller;

import com.giabao.linkflow.redirect.service.RedirectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final RedirectService redirectService;

    @GetMapping("/{shortCode:[A-Za-z0-9]{6,32}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        return redirectService.resolveActiveUrl(shortCode)
                .map(url -> ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).<Void>build())
                .orElseGet(() -> ResponseEntity.notFound().<Void>build());
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew test --tests 'com.giabao.linkflow.redirect.controller.RedirectControllerTest'`
Expected: PASS (2 tests).

- [ ] **Step 5: Run the full build to confirm nothing regressed**

Run: `docker compose up -d` (if not already running) then `./gradlew build`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/giabao/linkflow/redirect/controller/RedirectController.java \
        src/test/java/com/giabao/linkflow/redirect/controller/RedirectControllerTest.java
git commit -m "feat(redirect): add public GET /{shortCode} redirect endpoint"
```

---

## Notes & Deferred Items

- **No new Flyway migration** this slice — the `links` table and indexes already exist in `V1__init_schema.sql`.
- **`incrementClickCount` JPQL** is verified indirectly (service/redirect unit tests assert it is invoked) and is standard JPQL. A dedicated Postgres-backed `@DataJpaTest` covering the modifying query and confirming `updated_at` is untouched is a reasonable **future** addition but is out of scope here to stay consistent with the codebase's current unit-test-only style.
- **Out of scope (next slices):** `click_events` table + per-click event recording, analytics read APIs, Redis caching of `shortCode → originalUrl`, custom aliases / QR / custom domains.
```
