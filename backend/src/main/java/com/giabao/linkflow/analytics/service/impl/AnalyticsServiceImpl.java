package com.giabao.linkflow.analytics.service.impl;

import com.giabao.linkflow.analytics.dto.request.ClickEventFilterRequest;
import com.giabao.linkflow.analytics.dto.response.ClickEventResponse;
import com.giabao.linkflow.analytics.dto.response.ClickSummaryResponse;
import com.giabao.linkflow.analytics.mapper.ClickEventMapper;
import com.giabao.linkflow.analytics.repository.ClickEventRepository;
import com.giabao.linkflow.analytics.service.AnalyticsService;
import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.link.repository.LinkRepository;
import com.giabao.linkflow.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {
    private static final String LINK_NOT_FOUND_MESSAGE = "Link not found";

    private final ClickEventRepository clickEventRepository;
    private final ClickEventMapper clickEventMapper;
    private final LinkRepository linkRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public ClickSummaryResponse getSummary(UUID linkId) {
        verifyOwnership(linkId);

        return clickEventRepository.summarize(linkId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClickEventResponse> getClicks(UUID linkId, ClickEventFilterRequest request) {
        verifyOwnership(linkId);

        Page<ClickEventResponse> clicks = clickEventRepository
                .findByLinkId(
                        linkId,
                        request.toPageable(ClickEventFilterRequest.ALLOWED_SORT_FIELDS, ClickEventFilterRequest.DEFAULT_SORT_BY)
                )
                .map(clickEventMapper::toResponse);

        return PageResponse.from(clicks);
    }

    private void verifyOwnership(UUID linkId) {
        linkRepository.findByIdAndUserId(linkId, currentUserService.getCurrentUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, LINK_NOT_FOUND_MESSAGE));
    }
}
