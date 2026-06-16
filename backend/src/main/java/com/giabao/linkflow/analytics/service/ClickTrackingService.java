package com.giabao.linkflow.analytics.service;

import com.giabao.linkflow.analytics.entity.ClickEvent;
import com.giabao.linkflow.analytics.repository.ClickEventRepository;
import com.giabao.linkflow.common.util.HashingUtil;
import com.giabao.linkflow.link.entity.Link;
import com.giabao.linkflow.link.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Single place where a click's side effects are recorded: increment the link's
 * counter and persist a detailed {@link ClickEvent}. Stage 8 (Kafka) will replace
 * the body of {@link #recordClick} with an event publish without touching callers.
 */
@Service
@RequiredArgsConstructor
public class ClickTrackingService {

    private final LinkRepository linkRepository;
    private final ClickEventRepository clickEventRepository;

    @Transactional
    public void recordClick(Link link, ClickContext context) {
        linkRepository.incrementClickCount(link.getId());

        ClickEvent event = new ClickEvent();
        event.setLink(link);
        event.setClickedAt(Instant.now());
        event.setIpHash(hashIp(context.ipAddress()));
        event.setUserAgent(context.userAgent());
        event.setReferer(context.referer());

        clickEventRepository.save(event);
    }

    private String hashIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }

        return HashingUtil.sha256Hex(ipAddress);
    }
}
