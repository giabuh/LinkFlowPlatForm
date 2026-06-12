package com.giabao.linkflow.link.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giabao.linkflow.auth.service.RefreshSessionService;
import com.giabao.linkflow.auth.service.TokenService;
import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.link.dto.request.CreateLinkRequest;
import com.giabao.linkflow.link.dto.request.UpdateLinkRequest;
import com.giabao.linkflow.link.dto.response.LinkResponse;
import com.giabao.linkflow.link.service.LinkService;
import com.giabao.linkflow.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LinkController.class)
@AutoConfigureMockMvc(addFilters = false)
class LinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LinkService linkService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private RefreshSessionService refreshSessionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private LinkResponse sampleLink(UUID id) {
        return new LinkResponse(id, "https://example.com", "aB92xK7", "My link",
                0L, true, null, Instant.now(), Instant.now());
    }

    @Test
    void getMyLinksReturnsPagedLinks() throws Exception {
        UUID id = UUID.randomUUID();
        when(linkService.getMyLinks(any())).thenReturn(new PageResponse<>(
                1, 20, 1, 1, List.of(sampleLink(id))));

        mockMvc.perform(get("/api/v1/links").param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Links retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data.content[0].shortCode").value("aB92xK7"));
    }

    @Test
    void getLinkByIdReturnsLink() throws Exception {
        UUID id = UUID.randomUUID();
        when(linkService.getMyLink(id)).thenReturn(sampleLink(id));

        mockMvc.perform(get("/api/v1/links/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Link retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    void createLinkReturnsCreated() throws Exception {
        UUID id = UUID.randomUUID();
        CreateLinkRequest request = new CreateLinkRequest("https://example.com", "My link", null);
        when(linkService.createLink(any(CreateLinkRequest.class))).thenReturn(sampleLink(id));

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Link created successfully"))
                .andExpect(jsonPath("$.data.id").value(id.toString()));

        verify(linkService).createLink(any(CreateLinkRequest.class));
    }

    @Test
    void createLinkRejectsBlankUrl() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest("", "My link", null);

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.originalUrl").exists());

        verifyNoInteractions(linkService);
    }

    @Test
    void createLinkRejectsInvalidUrl() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest("not a url", "My link", null);

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.originalUrl").exists());

        verifyNoInteractions(linkService);
    }

    @Test
    void createLinkRejectsPastExpiry() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest(
                "https://example.com", "My link", Instant.now().minus(1, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.expiresAt").exists());

        verifyNoInteractions(linkService);
    }

    @Test
    void updateLinkReturnsUpdated() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateLinkRequest request = new UpdateLinkRequest("https://new.example.com", "New title", null);
        when(linkService.updateLink(any(UUID.class), any(UpdateLinkRequest.class))).thenReturn(sampleLink(id));

        mockMvc.perform(put("/api/v1/links/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Link updated successfully"));
    }

    @Test
    void activateLinkReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(linkService.activateLink(id)).thenReturn(sampleLink(id));

        mockMvc.perform(patch("/api/v1/links/{id}/activate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Link activated successfully"));
    }

    @Test
    void deactivateLinkReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(linkService.deactivateLink(id)).thenReturn(sampleLink(id));

        mockMvc.perform(patch("/api/v1/links/{id}/deactivate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Link deactivated successfully"));
    }

    @Test
    void deleteLinkReturnsSuccessEnvelope() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/links/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Link deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(linkService).deleteLink(id);
    }

    @Test
    void missingLinkReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(linkService.getMyLink(id)).thenThrow(new AppException(ErrorCode.NOT_FOUND, "Link not found"));

        mockMvc.perform(get("/api/v1/links/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Link not found"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
