package com.giabao.linkflow.auth.service;

import com.giabao.linkflow.auth.token.ParsedRefreshToken;
import com.giabao.linkflow.auth.dto.request.LoginRequest;
import com.giabao.linkflow.auth.dto.request.RegisterRequest;
import com.giabao.linkflow.auth.dto.response.LoginResponse;
import com.giabao.linkflow.auth.dto.response.TokenResponse;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.security.CurrentUserService;
import com.giabao.linkflow.security.CustomUserDetails;
import com.giabao.linkflow.security.CustomUserDetailsService;
import com.giabao.linkflow.user.dto.request.CreateUserRequest;
import com.giabao.linkflow.user.dto.response.UserResponse;
import com.giabao.linkflow.user.entity.UserRole;
import com.giabao.linkflow.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final org.springframework.security.authentication.AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final RefreshSessionService refreshSessionService;
    private final RefreshCookieService refreshCookieService;
    private final CustomUserDetailsService customUserDetailsService;
    private final CurrentUserService currentUserService;

    public UserResponse register(RegisterRequest request) {
        return userService.createUser(new CreateUserRequest(
                request.email(),
                request.password(),
                request.fullName(),
                UserRole.USER
        ));
    }

    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        UUID sessionId = UUID.randomUUID();
        String accessToken = tokenService.createAccessToken(user, sessionId);
        String refreshToken = tokenService.createRefreshToken(user, sessionId);

        refreshSessionService.save(sessionId, user.getId(), refreshToken);
        refreshCookieService.addRefreshCookie(response, refreshToken);

        return new LoginResponse(accessToken, userService.getUserById(user.getId()));
    }

    public UserResponse getCurrentUser() {
        return userService.getUserById(currentUserService.getCurrentUser().getId());
    }

    public TokenResponse refresh(Cookie[] cookies, HttpServletResponse response) {
        String refreshToken = refreshCookieService.extractRefreshToken(cookies)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
        ParsedRefreshToken parsedToken = tokenService.parseRefreshToken(refreshToken);

        refreshSessionService.findValidSession(parsedToken.sessionId(), refreshToken)
                .orElseThrow(() -> {
                    refreshCookieService.clearRefreshCookie(response);
                    return new AppException(ErrorCode.UNAUTHORIZED);
                });

        CustomUserDetails user = customUserDetailsService.loadUserById(parsedToken.userId());
        String accessToken = tokenService.createAccessToken(user, parsedToken.sessionId());
        String rotatedRefreshToken = tokenService.createRefreshToken(user, parsedToken.sessionId());
        refreshSessionService.save(parsedToken.sessionId(), user.getId(), rotatedRefreshToken);
        refreshCookieService.addRefreshCookie(response, rotatedRefreshToken);

        return new TokenResponse(accessToken);
    }

    public void logout(Cookie[] cookies, HttpServletResponse response) {
        refreshCookieService.extractRefreshToken(cookies).ifPresent(refreshToken -> {
            try {
                ParsedRefreshToken parsedToken = tokenService.parseRefreshToken(refreshToken);
                refreshSessionService.delete(parsedToken.sessionId());
            } catch (RuntimeException exception) {
                // Logout should still clear the client cookie even if the token is already invalid.
            }
        });

        refreshCookieService.clearRefreshCookie(response);
    }
}
