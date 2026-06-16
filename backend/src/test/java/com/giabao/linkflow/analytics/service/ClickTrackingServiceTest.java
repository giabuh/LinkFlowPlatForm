package com.giabao.linkflow.analytics.service;

import com.giabao.linkflow.analytics.entity.ClickEvent;
import com.giabao.linkflow.analytics.repository.ClickEventRepository;
import com.giabao.linkflow.link.entity.Link;
import com.giabao.linkflow.link.repository.LinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClickTrackingServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    @InjectMocks
    private ClickTrackingService clickTrackingService;

    private Link linkWithId(UUID id) {
        Link link = Mockito.spy(new Link());
        link.setOriginalUrl("https://example.com");
        when(link.getId()).thenReturn(id);
        return link;
    }

    @Test
    void recordClickIncrementsCountAndSavesEvent() {
        UUID linkId = UUID.randomUUID();
        Link link = linkWithId(linkId);
        ClickContext context = new ClickContext("1.2.3.4", "Mozilla/5.0", "https://google.com");

        clickTrackingService.recordClick(link, context);

        verify(linkRepository).incrementClickCount(linkId);

        ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(clickEventRepository).save(captor.capture());
        ClickEvent saved = captor.getValue();
        assertThat(saved.getLink()).isSameAs(link);
        assertThat(saved.getClickedAt()).isNotNull();
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(saved.getReferer()).isEqualTo("https://google.com");
        assertThat(saved.getIpHash()).hasSize(64);
        assertThat(saved.getIpHash()).isNotEqualTo("1.2.3.4");
    }

    @Test
    void recordClickLeavesIpHashNullWhenIpMissing() {
        UUID linkId = UUID.randomUUID();
        Link link = linkWithId(linkId);
        ClickContext context = new ClickContext(null, "Mozilla/5.0", null);

        clickTrackingService.recordClick(link, context);

        ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(clickEventRepository).save(captor.capture());
        assertThat(captor.getValue().getIpHash()).isNull();
    }
}
