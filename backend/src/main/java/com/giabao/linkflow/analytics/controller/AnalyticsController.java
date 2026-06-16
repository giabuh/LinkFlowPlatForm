package com.giabao.linkflow.analytics.controller;

import com.giabao.linkflow.analytics.dto.request.ClickEventFilterRequest;
import com.giabao.linkflow.analytics.dto.response.ClickEventResponse;
import com.giabao.linkflow.analytics.dto.response.ClickSummaryResponse;
import com.giabao.linkflow.analytics.service.AnalyticsService;
import com.giabao.linkflow.common.annotation.ApiMessage;
import com.giabao.linkflow.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/links/{linkId}/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    @ApiMessage("Link analytics summary retrieved successfully")
    public ResponseEntity<ClickSummaryResponse> getSummary(@PathVariable UUID linkId) {
        return ResponseEntity.ok(analyticsService.getSummary(linkId));
    }

    @GetMapping("/clicks")
    @ApiMessage("Link clicks retrieved successfully")
    public ResponseEntity<PageResponse<ClickEventResponse>> getClicks(
            @PathVariable UUID linkId,
            @ModelAttribute ClickEventFilterRequest request
    ) {
        return ResponseEntity.ok(analyticsService.getClicks(linkId, request));
    }
}
