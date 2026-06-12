package com.giabao.linkflow.redirect.service;

import com.giabao.linkflow.link.entity.Link;
import com.giabao.linkflow.link.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedirectService {

    private final LinkRepository linkRepository;

    @Transactional
    public Optional<String> resolveActiveUrl(String shortCode) {
        return linkRepository.findByShortCode(shortCode)
                .filter(this::isServable)
                .map(link -> {
                    linkRepository.incrementClickCount(link.getId());
                    return link.getOriginalUrl();
                });
    }

    private boolean isServable(Link link) {
        if (!Boolean.TRUE.equals(link.getActive())) {
            return false;
        }

        Instant expiresAt = link.getExpiresAt();
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }
}
