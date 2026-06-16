package com.giabao.linkflow.analytics.service;

import com.giabao.linkflow.analytics.dto.request.ClickEventFilterRequest;
import com.giabao.linkflow.analytics.dto.response.ClickEventResponse;
import com.giabao.linkflow.analytics.dto.response.ClickSummaryResponse;
import com.giabao.linkflow.common.dto.PageResponse;

import java.util.UUID;

public interface AnalyticsService {
    ClickSummaryResponse getSummary(UUID linkId);

    PageResponse<ClickEventResponse> getClicks(UUID linkId, ClickEventFilterRequest request);
}
