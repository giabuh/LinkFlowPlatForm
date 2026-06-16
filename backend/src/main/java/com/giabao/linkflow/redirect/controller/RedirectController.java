package com.giabao.linkflow.redirect.controller;

import com.giabao.linkflow.analytics.service.ClickContext;
import com.giabao.linkflow.common.constant.ShortCodePattern;
import com.giabao.linkflow.redirect.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String REFERER_HEADER = "Referer";

    private final RedirectService redirectService;

    @GetMapping("/{shortCode:" + ShortCodePattern.REGEX + "}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        ClickContext context = new ClickContext(
                clientIp(request),
                request.getHeader(USER_AGENT_HEADER),
                request.getHeader(REFERER_HEADER)
        );

        return redirectService.resolveActiveUrl(shortCode, context)
                .map(url -> ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).<Void>build())
                .orElseGet(() -> ResponseEntity.notFound().<Void>build());
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
