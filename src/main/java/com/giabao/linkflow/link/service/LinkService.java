package com.giabao.linkflow.link.service;

import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.link.dto.request.CreateLinkRequest;
import com.giabao.linkflow.link.dto.request.LinkFilterRequest;
import com.giabao.linkflow.link.dto.request.UpdateLinkRequest;
import com.giabao.linkflow.link.dto.response.LinkResponse;

import java.util.UUID;

public interface LinkService {
    PageResponse<LinkResponse> getMyLinks(LinkFilterRequest request);

    LinkResponse getMyLink(UUID id);

    LinkResponse createLink(CreateLinkRequest request);

    LinkResponse updateLink(UUID id, UpdateLinkRequest request);

    LinkResponse activateLink(UUID id);

    LinkResponse deactivateLink(UUID id);

    void deleteLink(UUID id);
}
