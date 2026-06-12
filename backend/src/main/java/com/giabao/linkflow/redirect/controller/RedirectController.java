package com.giabao.linkflow.redirect.controller;

import com.giabao.linkflow.redirect.service.RedirectService;
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

    private final RedirectService redirectService;

    @GetMapping("/{shortCode:[A-Za-z0-9]{6,32}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        return redirectService.resolveActiveUrl(shortCode)
                .map(url -> ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).<Void>build())
                .orElseGet(() -> ResponseEntity.notFound().<Void>build());
    }
}
