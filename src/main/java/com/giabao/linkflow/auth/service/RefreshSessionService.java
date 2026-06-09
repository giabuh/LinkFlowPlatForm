package com.giabao.linkflow.auth.service;

import com.giabao.linkflow.auth.config.JwtProperties;
import com.giabao.linkflow.auth.session.RefreshSession;
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
    private static final HexFormat HEX = HexFormat.of();

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

    public boolean exists(UUID sessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(sessionId)));
    }

    public void delete(UUID sessionId) {
        redisTemplate.delete(key(sessionId));
    }

    String hash(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return HEX.formatHex(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Refresh token hash algorithm is unavailable", exception);
        }
    }

    private String key(UUID sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
