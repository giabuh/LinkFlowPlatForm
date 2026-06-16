package com.giabao.linkflow.analytics.dto.response;

import java.time.Instant;

public record ClickSummaryResponse(
        long totalClicks,
        long uniqueClicks,
        Instant lastClickedAt
) {
}
