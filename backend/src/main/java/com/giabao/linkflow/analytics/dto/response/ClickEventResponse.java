package com.giabao.linkflow.analytics.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ClickEventResponse(
        UUID id,
        Instant clickedAt,
        String ipHash,
        String userAgent,
        String referer
) {
}
