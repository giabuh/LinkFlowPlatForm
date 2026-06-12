package com.giabao.linkflow.link.service;

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
import com.giabao.linkflow.link.service.impl.LinkServiceImpl;
import com.giabao.linkflow.security.CurrentUserService;
import com.giabao.linkflow.security.CustomUserDetails;
import com.giabao.linkflow.user.entity.User;
import com.giabao.linkflow.user.entity.UserRole;
import com.giabao.linkflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkServiceImplTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private LinkMapper linkMapper;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LinkServiceImpl linkService;

    private final UUID currentUserId = UUID.randomUUID();

    @BeforeEach
    void stubCurrentUser() {
        Mockito.lenient().when(currentUserService.getCurrentUser())
                .thenReturn(new CustomUserDetails(currentUserId, UserRole.USER));
    }

    private Specification<Link> anySpecification() {
        return any();
    }

    @Test
    void createLinkAssignsShortCodeAndOwner() {
        CreateLinkRequest request = new CreateLinkRequest("https://example.com", "My link", null);
        Link mappedLink = new Link();
        User ownerRef = new User();
        LinkResponse response = new LinkResponse(
                UUID.randomUUID(), "https://example.com", "aB92xK7", "My link",
                0L, true, null, Instant.now(), Instant.now());

        when(linkMapper.toEntity(request)).thenReturn(mappedLink);
        when(shortCodeGenerator.generate()).thenReturn("aB92xK7");
        when(userRepository.getReferenceById(currentUserId)).thenReturn(ownerRef);
        when(linkRepository.save(mappedLink)).thenReturn(mappedLink);
        when(linkMapper.toResponse(mappedLink)).thenReturn(response);

        LinkResponse actual = linkService.createLink(request);

        assertThat(actual).isSameAs(response);
        ArgumentCaptor<Link> linkCaptor = ArgumentCaptor.forClass(Link.class);
        verify(linkRepository).save(linkCaptor.capture());
        Link saved = linkCaptor.getValue();
        assertThat(saved.getShortCode()).isEqualTo("aB92xK7");
        assertThat(saved.getUser()).isSameAs(ownerRef);
    }

    @Test
    void getMyLinkReturnsOwnedLink() {
        UUID linkId = UUID.randomUUID();
        Link link = new Link();
        LinkResponse response = new LinkResponse(
                linkId, "https://example.com", "aB92xK7", "My link",
                3L, true, null, Instant.now(), Instant.now());

        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.of(link));
        when(linkMapper.toResponse(link)).thenReturn(response);

        assertThat(linkService.getMyLink(linkId)).isSameAs(response);
    }

    @Test
    void getMyLinkThrowsNotFoundWhenNotOwned() {
        UUID linkId = UUID.randomUUID();
        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linkService.getMyLink(linkId))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND)
                )
                .hasMessage("Link not found");
    }

    @Test
    void updateLinkUpdatesOwnedLink() {
        UUID linkId = UUID.randomUUID();
        Link link = new Link();
        UpdateLinkRequest request = new UpdateLinkRequest("https://new.example.com", "New title", null);
        LinkResponse response = new LinkResponse(
                linkId, "https://new.example.com", "aB92xK7", "New title",
                0L, true, null, Instant.now(), Instant.now());

        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.of(link));
        when(linkRepository.save(link)).thenReturn(link);
        when(linkMapper.toResponse(link)).thenReturn(response);

        assertThat(linkService.updateLink(linkId, request)).isSameAs(response);
        verify(linkMapper).updateEntity(request, link);
    }

    @Test
    void activateLinkSetsActiveTrue() {
        UUID linkId = UUID.randomUUID();
        Link link = new Link();
        link.setActive(false);

        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.of(link));
        when(linkRepository.save(link)).thenReturn(link);
        when(linkMapper.toResponse(link)).thenReturn(
                new LinkResponse(linkId, "https://example.com", "aB92xK7", null,
                        0L, true, null, Instant.now(), Instant.now()));

        linkService.activateLink(linkId);

        assertThat(link.getActive()).isTrue();
        verify(linkRepository).save(link);
    }

    @Test
    void deactivateLinkSetsActiveFalse() {
        UUID linkId = UUID.randomUUID();
        Link link = new Link();
        link.setActive(true);

        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.of(link));
        when(linkRepository.save(link)).thenReturn(link);
        when(linkMapper.toResponse(link)).thenReturn(
                new LinkResponse(linkId, "https://example.com", "aB92xK7", null,
                        0L, false, null, Instant.now(), Instant.now()));

        linkService.deactivateLink(linkId);

        assertThat(link.getActive()).isFalse();
        verify(linkRepository).save(link);
    }

    @Test
    void deleteLinkRemovesOwnedLink() {
        UUID linkId = UUID.randomUUID();
        Link link = new Link();
        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.of(link));

        linkService.deleteLink(linkId);

        verify(linkRepository).delete(link);
    }

    @Test
    void deleteLinkThrowsNotFoundWhenNotOwned() {
        UUID linkId = UUID.randomUUID();
        when(linkRepository.findByIdAndUserId(linkId, currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linkService.deleteLink(linkId))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND)
                );

        verify(linkRepository, never()).delete(any(Link.class));
    }

    @Test
    void getMyLinksUsesSpecificationAndPageable() {
        LinkFilterRequest request = new LinkFilterRequest();
        request.setPage(1);
        request.setSize(10);
        request.setSortBy("clickCount");
        request.setSortDirection("desc");
        request.setTitle("promo");
        request.setActive(true);

        Link link = new Link();
        LinkResponse response = new LinkResponse(
                UUID.randomUUID(), "https://example.com", "aB92xK7", "promo",
                9L, true, null, Instant.now(), Instant.now());

        when(linkRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(link),
                        request.toPageable(LinkFilterRequest.ALLOWED_SORT_FIELDS, LinkFilterRequest.DEFAULT_SORT_BY),
                        21));
        when(linkMapper.toResponse(link)).thenReturn(response);

        PageResponse<LinkResponse> actual = linkService.getMyLinks(request);

        assertThat(actual.content()).containsExactly(response);
        assertThat(actual.totalElements()).isEqualTo(21);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(linkRepository).findAll(anySpecification(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "clickCount"));
    }
}
