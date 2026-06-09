package com.giabao.linkflow.auth.service;

import com.giabao.linkflow.auth.config.JwtProperties;
import com.giabao.linkflow.auth.token.ParsedAccessToken;
import com.giabao.linkflow.auth.token.ParsedRefreshToken;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.security.CustomUserDetails;
import com.giabao.linkflow.user.entity.User;
import com.giabao.linkflow.user.entity.UserRole;
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
    void createsAndParsesAccessTokenWithUserSubjectRoleAndSession() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        CustomUserDetails user = userDetails(userId, UserRole.ADMIN);

        String token = tokenService.createAccessToken(user, sessionId);

        ParsedAccessToken parsed = tokenService.parseAccessToken(token);
        assertThat(parsed.userId()).isEqualTo(userId);
        assertThat(parsed.role()).isEqualTo(UserRole.ADMIN.name());
        assertThat(parsed.sessionId()).isEqualTo(sessionId);
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
        String token = tokenService.createAccessToken(userDetails(UUID.randomUUID(), UserRole.USER), UUID.randomUUID());
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
