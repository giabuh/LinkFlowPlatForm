package com.giabao.linkflow.auth.token;

import java.util.UUID;

public record ParsedRefreshToken(UUID userId, UUID sessionId) {
}
