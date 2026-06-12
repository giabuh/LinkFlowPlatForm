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
