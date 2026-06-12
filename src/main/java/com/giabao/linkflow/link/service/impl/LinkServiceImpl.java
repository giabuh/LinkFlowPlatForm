package com.giabao.linkflow.link.service.impl;

import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.link.dto.request.CreateLinkRequest;
import com.giabao.linkflow.link.dto.request.LinkFilterRequest;
import com.giabao.linkflow.link.dto.request.UpdateLinkRequest;
import com.giabao.linkflow.link.dto.response.LinkResponse;
import com.giabao.linkflow.link.entity.Link;
import com.giabao.linkflow.link.mapper.LinkMapper;
import com.giabao.linkflow.link.repository.LinkRepository;
import com.giabao.linkflow.link.service.LinkService;
import com.giabao.linkflow.link.service.ShortCodeGenerator;
import com.giabao.linkflow.link.specification.LinkSpecifications;
import com.giabao.linkflow.security.CurrentUserService;
import com.giabao.linkflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LinkServiceImpl implements LinkService {
    private static final String LINK_NOT_FOUND_MESSAGE = "Link not found";

    private final LinkRepository linkRepository;
    private final LinkMapper linkMapper;
    private final ShortCodeGenerator shortCodeGenerator;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LinkResponse> getMyLinks(LinkFilterRequest request) {
        Page<LinkResponse> links = linkRepository
                .findAll(
                        LinkSpecifications.byFilter(currentUserId(), request),
                        request.toPageable(LinkFilterRequest.ALLOWED_SORT_FIELDS, LinkFilterRequest.DEFAULT_SORT_BY)
                )
                .map(linkMapper::toResponse);

        return PageResponse.from(links);
    }

    @Override
    @Transactional(readOnly = true)
    public LinkResponse getMyLink(UUID id) {
        return linkMapper.toResponse(findOwnedLink(id));
    }

    @Override
    @Transactional
    public LinkResponse createLink(CreateLinkRequest request) {
        Link link = linkMapper.toEntity(request);
        link.setUser(userRepository.getReferenceById(currentUserId()));
        link.setShortCode(shortCodeGenerator.generate());

        return linkMapper.toResponse(linkRepository.save(link));
    }

    @Override
    @Transactional
    public LinkResponse updateLink(UUID id, UpdateLinkRequest request) {
        Link link = findOwnedLink(id);
        linkMapper.updateEntity(request, link);

        return linkMapper.toResponse(linkRepository.save(link));
    }

    @Override
    @Transactional
    public LinkResponse activateLink(UUID id) {
        return setActive(id, true);
    }

    @Override
    @Transactional
    public LinkResponse deactivateLink(UUID id) {
        return setActive(id, false);
    }

    @Override
    @Transactional
    public void deleteLink(UUID id) {
        linkRepository.delete(findOwnedLink(id));
    }

    private LinkResponse setActive(UUID id, boolean active) {
        Link link = findOwnedLink(id);
        link.setActive(active);

        return linkMapper.toResponse(linkRepository.save(link));
    }

    private Link findOwnedLink(UUID id) {
        return linkRepository.findByIdAndUserId(id, currentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, LINK_NOT_FOUND_MESSAGE));
    }

    private UUID currentUserId() {
        return currentUserService.getCurrentUser().getId();
    }
}
