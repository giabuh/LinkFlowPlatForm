package com.giabao.linkflow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giabao.linkflow.auth.dto.request.LoginRequest;
import com.giabao.linkflow.auth.dto.request.RegisterRequest;
import com.giabao.linkflow.auth.dto.response.LoginResponse;
import com.giabao.linkflow.auth.dto.response.TokenResponse;
import com.giabao.linkflow.auth.service.AuthService;
import com.giabao.linkflow.auth.service.RefreshSessionService;
import com.giabao.linkflow.auth.service.TokenService;
import com.giabao.linkflow.security.CustomUserDetailsService;
import com.giabao.linkflow.user.dto.response.UserResponse;
import com.giabao.linkflow.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private RefreshSessionService refreshSessionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void registerReturnsCreatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        RegisterRequest request = new RegisterRequest("ada@example.com", "secret-password", "Ada Lovelace");
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new UserResponse(userId, "ada@example.com", "Ada Lovelace", UserRole.USER));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("ada@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void registerRejectsInvalidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest("bad-email", "short", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.password").exists())
                .andExpect(jsonPath("$.data.fullName").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void loginReturnsAccessTokenAndUser() throws Exception {
        UUID userId = UUID.randomUUID();
        LoginRequest request = new LoginRequest("ada@example.com", "secret-password");
        UserResponse user = new UserResponse(userId, "ada@example.com", "Ada Lovelace", UserRole.USER);
        when(authService.login(any(LoginRequest.class), any())).thenReturn(new LoginResponse("access-token", user));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User logged in successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.user.email").value("ada@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("USER"));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authService.getCurrentUser())
                .thenReturn(new UserResponse(userId, "ada@example.com", "Ada Lovelace", UserRole.USER));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Current user retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("ada@example.com"));
    }

    @Test
    void refreshReturnsAccessToken() throws Exception {
        when(authService.refresh(any(), any())).thenReturn(new TokenResponse("new-access-token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new MockCookie("refresh_token", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Access token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }

    @Test
    void logoutReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new MockCookie("refresh_token", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User logged out successfully"));
    }
}
