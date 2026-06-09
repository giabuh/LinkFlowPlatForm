package com.giabao.linkflow.auth.service;

import com.giabao.linkflow.auth.config.JwtProperties;
import com.giabao.linkflow.auth.session.RefreshSession;
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
import static org.mockito.ArgumentMatchers.eq;
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
                eq("auth:refresh-session:" + sessionId),
                sessionCaptor.capture(),
                eq(Duration.ofMillis(604800000))
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

    @Test
    void existsReturnsTrueWhenSessionPresent() {
        RefreshSessionService service = service();
        UUID sessionId = UUID.randomUUID();
        when(redisTemplate.hasKey("auth:refresh-session:" + sessionId)).thenReturn(true);

        assertThat(service.exists(sessionId)).isTrue();
    }

    @Test
    void existsReturnsFalseWhenSessionAbsent() {
        RefreshSessionService service = service();
        UUID sessionId = UUID.randomUUID();
        when(redisTemplate.hasKey("auth:refresh-session:" + sessionId)).thenReturn(false);

        assertThat(service.exists(sessionId)).isFalse();
    }

    private RefreshSessionService service() {
        return new RefreshSessionService(redisTemplate, new JwtProperties(
                "12345678901234567890123456789012",
                900000,
                604800000
        ));
    }
}
