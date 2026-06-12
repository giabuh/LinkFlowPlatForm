package com.giabao.linkflow.redirect.controller;

import com.giabao.linkflow.auth.service.RefreshSessionService;
import com.giabao.linkflow.auth.service.TokenService;
import com.giabao.linkflow.redirect.service.RedirectService;
import com.giabao.linkflow.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RedirectController.class)
@AutoConfigureMockMvc(addFilters = false)
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RedirectService redirectService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private RefreshSessionService refreshSessionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void redirectsWithFoundAndLocationHeaderAndNoEnvelope() throws Exception {
        when(redirectService.resolveActiveUrl("aB92xK7")).thenReturn(Optional.of("https://example.com"));

        mockMvc.perform(get("/aB92xK7"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"))
                .andExpect(content().string(""));
    }

    @Test
    void returnsPlainNotFoundWhenUnresolved() throws Exception {
        when(redirectService.resolveActiveUrl("aB92xK7")).thenReturn(Optional.empty());

        mockMvc.perform(get("/aB92xK7"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }
}
