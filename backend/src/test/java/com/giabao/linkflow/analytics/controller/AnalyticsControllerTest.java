package com.giabao.linkflow.analytics.controller;

import com.giabao.linkflow.analytics.dto.response.ClickEventResponse;
import com.giabao.linkflow.analytics.dto.response.ClickSummaryResponse;
import com.giabao.linkflow.analytics.service.AnalyticsService;
import com.giabao.linkflow.auth.service.RefreshSessionService;
import com.giabao.linkflow.auth.service.TokenService;
import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private RefreshSessionService refreshSessionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getSummaryReturnsEnvelope() throws Exception {
        UUID linkId = UUID.randomUUID();
        when(analyticsService.getSummary(linkId))
                .thenReturn(new ClickSummaryResponse(10L, 7L, Instant.parse("2026-06-15T10:00:00Z")));

        mockMvc.perform(get("/api/v1/links/{linkId}/analytics/summary", linkId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Link analytics summary retrieved successfully"))
                .andExpect(jsonPath("$.data.totalClicks").value(10))
                .andExpect(jsonPath("$.data.uniqueClicks").value(7));
    }

    @Test
    void getClicksReturnsPagedEnvelope() throws Exception {
        UUID linkId = UUID.randomUUID();
        UUID clickId = UUID.randomUUID();
        ClickEventResponse click = new ClickEventResponse(
                clickId, Instant.parse("2026-06-15T10:00:00Z"), "hash", "Mozilla/5.0", "https://google.com");
        when(analyticsService.getClicks(eq(linkId), any()))
                .thenReturn(new PageResponse<>(1, 20, 1, 1, List.of(click)));

        mockMvc.perform(get("/api/v1/links/{linkId}/analytics/clicks", linkId)
                        .param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Link clicks retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].id").value(clickId.toString()))
                .andExpect(jsonPath("$.data.content[0].userAgent").value("Mozilla/5.0"));
    }

    @Test
    void getSummaryForUnownedLinkReturnsNotFound() throws Exception {
        UUID linkId = UUID.randomUUID();
        when(analyticsService.getSummary(linkId))
                .thenThrow(new AppException(ErrorCode.NOT_FOUND, "Link not found"));

        mockMvc.perform(get("/api/v1/links/{linkId}/analytics/summary", linkId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Link not found"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
