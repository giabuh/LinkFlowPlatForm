package com.giabao.linkflow.auth.token;

import java.util.UUID;

public record ParsedAccessToken(UUID userId, String role, UUID sessionId) {
}
