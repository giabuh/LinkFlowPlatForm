package com.giabao.linkflow.redirect.service;

import com.giabao.linkflow.analytics.service.ClickContext;
import com.giabao.linkflow.analytics.service.ClickTrackingService;
import com.giabao.linkflow.link.entity.Link;
import com.giabao.linkflow.link.repository.LinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private ClickTrackingService clickTrackingService;

    @InjectMocks
    private RedirectService redirectService;

    private final ClickContext context = new ClickContext("1.2.3.4", "Mozilla/5.0", "https://google.com");

    private Link link(boolean active, Instant expiresAt) {
        Link link = new Link();
        link.setOriginalUrl("https://example.com");
        link.setActive(active);
        link.setExpiresAt(expiresAt);
        return link;
    }

    @Test
    void resolvesActiveLinkAndRecordsClick() {
        Link link = link(true, null);
        when(linkRepository.findByShortCode("aB92xK7")).thenReturn(Optional.of(link));

        Optional<String> url = redirectService.resolveActiveUrl("aB92xK7", context);

        assertThat(url).contains("https://example.com");
        verify(clickTrackingService).recordClick(link, context);
    }

    @Test
    void returnsEmptyWhenMissing() {
        when(linkRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThat(redirectService.resolveActiveUrl("missing", context)).isEmpty();
        verify(clickTrackingService, never()).recordClick(any(), any());
    }

    @Test
    void returnsEmptyWhenInactive() {
        when(linkRepository.findByShortCode("aB92xK7")).thenReturn(Optional.of(link(false, null)));

        assertThat(redirectService.resolveActiveUrl("aB92xK7", context)).isEmpty();
        verify(clickTrackingService, never()).recordClick(any(), any());
    }

    @Test
    void returnsEmptyWhenExpired() {
        Link expired = link(true, Instant.now().minus(1, ChronoUnit.HOURS));
        when(linkRepository.findByShortCode("aB92xK7")).thenReturn(Optional.of(expired));

        assertThat(redirectService.resolveActiveUrl("aB92xK7", context)).isEmpty();
        verify(clickTrackingService, never()).recordClick(any(), any());
    }
}
