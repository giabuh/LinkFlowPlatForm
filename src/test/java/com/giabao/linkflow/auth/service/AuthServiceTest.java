package com.giabao.linkflow.auth.service;

import com.giabao.linkflow.auth.token.ParsedRefreshToken;
import com.giabao.linkflow.auth.session.RefreshSession;
import com.giabao.linkflow.auth.dto.request.LoginRequest;
import com.giabao.linkflow.auth.dto.request.RegisterRequest;
import com.giabao.linkflow.auth.dto.response.TokenResponse;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.security.CustomUserDetails;
import com.giabao.linkflow.security.CustomUserDetailsService;
import com.giabao.linkflow.user.dto.request.CreateUserRequest;
import com.giabao.linkflow.user.dto.response.UserResponse;
import com.giabao.linkflow.user.entity.User;
import com.giabao.linkflow.user.entity.UserRole;
import com.giabao.linkflow.user.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private RefreshSessionService refreshSessionService;

    @Mock
    private RefreshCookieService refreshCookieService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private AuthService authService;

    private final MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();

    @Test
    void registerCreatesUserWithUserRole() {
        RegisterRequest request = new RegisterRequest("ada@example.com", "secret-password", "Ada Lovelace");
        UserResponse userResponse = new UserResponse(UUID.randomUUID(), "ada@example.com", "Ada Lovelace", UserRole.USER);
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(userResponse);

        UserResponse actual = authService.register(request);

        assertThat(actual).isSameAs(userResponse);
        ArgumentCaptor<CreateUserRequest> captor = ArgumentCaptor.forClass(CreateUserRequest.class);
        verify(userService).createUser(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("ada@example.com");
        assertThat(captor.getValue().password()).isEqualTo("secret-password");
        assertThat(captor.getValue().fullName()).isEqualTo("Ada Lovelace");
        assertThat(captor.getValue().role()).isEqualTo(UserRole.USER);
    }

    @Test
    void loginAuthenticatesWithAuthenticationManagerAndCreatesRefreshSession() throws Exception {
        CustomUserDetails userDetails = userDetails(UUID.randomUUID(), UserRole.USER);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        LoginRequest request = new LoginRequest("ada@example.com", "secret-password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(tokenService.createAccessToken(eq(userDetails), any(UUID.class))).thenReturn("access-token");
        when(tokenService.createRefreshToken(eq(userDetails), any(UUID.class))).thenReturn("refresh-token");

        TokenResponse response = authService.login(request, httpServletResponse);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshSessionService).save(any(UUID.class), eq(userDetails.getId()), eq("refresh-token"));
        verify(refreshCookieService).addRefreshCookie(httpServletResponse, "refresh-token");
    }

    @Test
    void refreshRotatesRefreshTokenWhenRedisSessionMatches() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Cookie[] cookies = {new Cookie("refresh_token", "old-refresh-token")};
        CustomUserDetails userDetails = userDetails(userId, UserRole.USER);
        when(refreshCookieService.extractRefreshToken(cookies)).thenReturn(Optional.of("old-refresh-token"));
        when(tokenService.parseRefreshToken("old-refresh-token")).thenReturn(new ParsedRefreshToken(userId, sessionId));
        when(refreshSessionService.findValidSession(sessionId, "old-refresh-token"))
                .thenReturn(Optional.of(new RefreshSession(userId, "hash")));
        when(customUserDetailsService.loadUserById(userId)).thenReturn(userDetails);
        when(tokenService.createAccessToken(userDetails, sessionId)).thenReturn("new-access-token");
        when(tokenService.createRefreshToken(userDetails, sessionId)).thenReturn("new-refresh-token");

        TokenResponse response = authService.refresh(cookies, httpServletResponse);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        verify(refreshSessionService).save(sessionId, userId, "new-refresh-token");
        verify(refreshCookieService).addRefreshCookie(httpServletResponse, "new-refresh-token");
    }

    @Test
    void refreshRejectsMissingCookie() {
        when(refreshCookieService.extractRefreshToken(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(null, httpServletResponse))
                .isInstanceOf(AppException.class);
    }

    @Test
    void logoutDeletesCurrentSessionAndClearsCookie() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Cookie[] cookies = {new Cookie("refresh_token", "refresh-token")};
        when(refreshCookieService.extractRefreshToken(cookies)).thenReturn(Optional.of("refresh-token"));
        when(tokenService.parseRefreshToken("refresh-token")).thenReturn(new ParsedRefreshToken(userId, sessionId));

        authService.logout(cookies, httpServletResponse);

        verify(refreshSessionService).delete(sessionId);
        verify(refreshCookieService).clearRefreshCookie(httpServletResponse);
    }

    private CustomUserDetails userDetails(UUID id, UserRole role) throws Exception {
        User user = new User();
        setId(user, id);
        user.setEmail("ada@example.com");
        user.setPassword("hash");
        user.setRole(role);
        return new CustomUserDetails(user);
    }

    private void setId(User user, UUID id) throws Exception {
        Field field = user.getClass().getSuperclass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}
