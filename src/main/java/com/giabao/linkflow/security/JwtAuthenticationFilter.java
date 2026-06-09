package com.giabao.linkflow.security;

import com.giabao.linkflow.auth.token.ParsedAccessToken;
import com.giabao.linkflow.auth.service.RefreshSessionService;
import com.giabao.linkflow.auth.service.TokenService;
import com.giabao.linkflow.user.entity.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final RefreshSessionService refreshSessionService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractBearerToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            ParsedAccessToken parsedToken = tokenService.parseAccessToken(token);
            if (!refreshSessionService.exists(parsedToken.sessionId())) {
                // Session was revoked (logout / refresh-token reuse): the access token is
                // cryptographically valid but its session is gone, so refuse to authenticate.
                SecurityContextHolder.clearContext();
                return;
            }
            CustomUserDetails userDetails = new CustomUserDetails(
                    parsedToken.userId(),
                    UserRole.valueOf(parsedToken.role())
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException exception) {
            // Invalid/expired/malformed token: stay unauthenticated so the request reaches the
            // authorization filter and is rejected with a clean 401 by RestAuthenticationEntryPoint
            // instead of letting the exception escape the filter chain as a 500.
            SecurityContextHolder.clearContext();
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
