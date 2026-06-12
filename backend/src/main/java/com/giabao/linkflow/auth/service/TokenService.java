package com.giabao.linkflow.auth.service;

import com.giabao.linkflow.auth.config.JwtProperties;
import com.giabao.linkflow.auth.token.ParsedAccessToken;
import com.giabao.linkflow.auth.token.ParsedRefreshToken;
import com.giabao.linkflow.auth.token.TokenType;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.security.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
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
    private final JwtParser jwtParser;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public TokenService(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser().verifyWith(secretKey).build();
        this.accessTokenExpirationMs = jwtProperties.accessTokenExpirationMs();
        this.refreshTokenExpirationMs = jwtProperties.refreshTokenExpirationMs();
    }

    public String createAccessToken(CustomUserDetails user, UUID sessionId) {
        return baseToken(user, TokenType.ACCESS, sessionId, accessTokenExpirationMs)
                .claim(ROLE_CLAIM, user.getRole().name())
                .compact();
    }

    public String createRefreshToken(CustomUserDetails user, UUID sessionId) {
        return baseToken(user, TokenType.REFRESH, sessionId, refreshTokenExpirationMs)
                .compact();
    }

    private JwtBuilder baseToken(CustomUserDetails user, TokenType type, UUID sessionId, long expirationMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(TOKEN_TYPE_CLAIM, type.name())
                .claim(SESSION_ID_CLAIM, sessionId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(secretKey, Jwts.SIG.HS256);
    }

    public ParsedAccessToken parseAccessToken(String token) {
        Claims claims = parseAndVerify(token);
        requireTokenType(claims, TokenType.ACCESS);
        return new ParsedAccessToken(
                parseUuid(claims.getSubject()),
                claims.get(ROLE_CLAIM, String.class),
                parseUuid(claims.get(SESSION_ID_CLAIM, String.class))
        );
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
            return jwtParser.parseSignedClaims(token).getPayload();
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
