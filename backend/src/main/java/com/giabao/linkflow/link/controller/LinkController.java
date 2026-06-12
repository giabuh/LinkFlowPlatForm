package com.giabao.linkflow.link.controller;

import com.giabao.linkflow.common.annotation.ApiMessage;
import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.link.dto.request.CreateLinkRequest;
import com.giabao.linkflow.link.dto.request.LinkFilterRequest;
import com.giabao.linkflow.link.dto.request.UpdateLinkRequest;
import com.giabao.linkflow.link.dto.response.LinkResponse;
import com.giabao.linkflow.link.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @GetMapping
    @ApiMessage("Links retrieved successfully")
    public ResponseEntity<PageResponse<LinkResponse>> getMyLinks(@ModelAttribute LinkFilterRequest request) {
        return ResponseEntity.ok(linkService.getMyLinks(request));
    }

    @GetMapping("/{id}")
    @ApiMessage("Link retrieved successfully")
    public ResponseEntity<LinkResponse> getMyLink(@PathVariable UUID id) {
        return ResponseEntity.ok(linkService.getMyLink(id));
    }

    @PostMapping
    @ApiMessage("Link created successfully")
    public ResponseEntity<LinkResponse> createLink(@Valid @RequestBody CreateLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(linkService.createLink(request));
    }

    @PutMapping("/{id}")
    @ApiMessage("Link updated successfully")
    public ResponseEntity<LinkResponse> updateLink(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLinkRequest request
    ) {
        return ResponseEntity.ok(linkService.updateLink(id, request));
    }

    @PatchMapping("/{id}/activate")
    @ApiMessage("Link activated successfully")
    public ResponseEntity<LinkResponse> activateLink(@PathVariable UUID id) {
        return ResponseEntity.ok(linkService.activateLink(id));
    }

    @PatchMapping("/{id}/deactivate")
    @ApiMessage("Link deactivated successfully")
    public ResponseEntity<LinkResponse> deactivateLink(@PathVariable UUID id) {
        return ResponseEntity.ok(linkService.deactivateLink(id));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Link deleted successfully")
    public ResponseEntity<Void> deleteLink(@PathVariable UUID id) {
        linkService.deleteLink(id);
        return ResponseEntity.ok().build();
    }
}
