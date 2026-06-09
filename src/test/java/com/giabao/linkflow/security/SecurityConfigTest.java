package com.giabao.linkflow.security;

import com.giabao.linkflow.auth.token.ParsedAccessToken;
import com.giabao.linkflow.auth.service.RefreshSessionService;
import com.giabao.linkflow.auth.service.TokenService;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ApiErrorWriter;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.user.controller.UserController;
import com.giabao.linkflow.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private RefreshSessionService refreshSessionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    void usersEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void invalidAccessTokenReturnsCleanUnauthorizedJson() throws Exception {
        when(tokenService.parseAccessToken("invalid-token"))
                .thenThrow(new AppException(ErrorCode.UNAUTHORIZED, "Invalid or expired token"));

        mockMvc.perform(get("/users").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void validAccessTokenWithRevokedSessionReturnsUnauthorized() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(tokenService.parseAccessToken("valid-token")).thenReturn(new ParsedAccessToken(userId, "ADMIN", sessionId));
        when(refreshSessionService.exists(sessionId)).thenReturn(false);

        mockMvc.perform(get("/users").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
