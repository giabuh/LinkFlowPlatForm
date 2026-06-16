package com.giabao.linkflow.analytics.service;

import com.giabao.linkflow.analytics.dto.request.ClickEventFilterRequest;
import com.giabao.linkflow.analytics.dto.response.ClickEventResponse;
import com.giabao.linkflow.analytics.dto.response.ClickSummaryResponse;
import com.giabao.linkflow.analytics.entity.ClickEvent;
import com.giabao.linkflow.analytics.mapper.ClickEventMapper;
import com.giabao.linkflow.analytics.repository.ClickEventRepository;
import com.giabao.linkflow.analytics.service.impl.AnalyticsServiceImpl;
import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.link.entity.Link;
import com.giabao.linkflow.link.repository.LinkRepository;
import com.giabao.linkflow.security.CurrentUserService;
import com.giabao.linkflow.security.CustomUserDetails;
import com.giabao.linkflow.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private ClickEventRepository clickEventRepository;

    @Mock
    private ClickEventMapper clickEventMapper;

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private final UUID currentUserId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        Mockito.lenient().when(currentUserService.getCurrentUser())
                .thenReturn(new CustomUserDetails(currentUserId, UserRole.USER));
    }

    @Test
    void getSummaryReturnsAggregatesForOwnedLink() {
        UUID linkId = UUID.randomUUID();
        Instant lastClick = Instant.now();
        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.of(new Link()));
        when(clickEventRepository.summarize(linkId))
                .thenReturn(new ClickSummaryResponse(10L, 7L, lastClick));

        ClickSummaryResponse response = analyticsService.getSummary(linkId);

        assertThat(response.totalClicks()).isEqualTo(10L);
        assertThat(response.uniqueClicks()).isEqualTo(7L);
        assertThat(response.lastClickedAt()).isEqualTo(lastClick);
    }

    @Test
    void getSummaryThrowsNotFoundWhenNotOwned() {
        UUID linkId = UUID.randomUUID();
        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.getSummary(linkId))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND))
                .hasMessage("Link not found");

        verify(clickEventRepository, never()).summarize(any());
    }

    @Test
    void getClicksReturnsPageForOwnedLink() {
        UUID linkId = UUID.randomUUID();
        ClickEventFilterRequest request = new ClickEventFilterRequest();
        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.of(new Link()));

        ClickEvent event = new ClickEvent();
        ClickEventResponse response = new ClickEventResponse(
                UUID.randomUUID(), Instant.now(), "hash", "Mozilla/5.0", "https://google.com");
        when(clickEventRepository.findByLinkId(eq(linkId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), Pageable.ofSize(20), 1));
        when(clickEventMapper.toResponse(event)).thenReturn(response);

        PageResponse<ClickEventResponse> page = analyticsService.getClicks(linkId, request);

        assertThat(page.content()).containsExactly(response);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void getClicksThrowsNotFoundWhenNotOwned() {
        UUID linkId = UUID.randomUUID();
        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.getClicks(linkId, new ClickEventFilterRequest()))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

        verify(clickEventRepository, never()).findByLinkId(any(), any());
    }
}
