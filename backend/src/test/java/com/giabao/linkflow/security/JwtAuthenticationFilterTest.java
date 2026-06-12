package com.giabao.linkflow.security;

import com.giabao.linkflow.auth.token.ParsedAccessToken;
import com.giabao.linkflow.auth.service.RefreshSessionService;
import com.giabao.linkflow.auth.service.TokenService;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.user.entity.UserRole;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private RefreshSessionService refreshSessionService;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAuthenticationFromAccessTokenClaimsWhenSessionIsAlive() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(tokenService.parseAccessToken("access-token")).thenReturn(new ParsedAccessToken(userId, "ADMIN", sessionId));
        when(refreshSessionService.exists(sessionId)).thenReturn(true);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, refreshSessionService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        assertThat(principal.getId()).isEqualTo(userId);
        assertThat(principal.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void rejectsWhenSessionWasRevoked() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(tokenService.parseAccessToken("access-token")).thenReturn(new ParsedAccessToken(userId, "ADMIN", sessionId));
        when(refreshSessionService.exists(sessionId)).thenReturn(false);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, refreshSessionService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void skipsWhenBearerTokenIsMissing() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, refreshSessionService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(tokenService, refreshSessionService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void leavesContextUnauthenticatedAndContinuesWhenTokenIsInvalid() throws Exception {
        when(tokenService.parseAccessToken("bad-token"))
                .thenThrow(new AppException(ErrorCode.UNAUTHORIZED, "Invalid or expired token"));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, refreshSessionService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(refreshSessionService);
        verify(filterChain).doFilter(request, response);
    }
}
