package com.giabao.linkflow.auth.controller;

import com.giabao.linkflow.auth.dto.request.LoginRequest;
import com.giabao.linkflow.auth.dto.request.RegisterRequest;
import com.giabao.linkflow.auth.dto.response.LoginResponse;
import com.giabao.linkflow.auth.dto.response.TokenResponse;
import com.giabao.linkflow.auth.service.AuthService;
import com.giabao.linkflow.common.annotation.ApiMessage;
import com.giabao.linkflow.user.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @ApiMessage("User registered successfully")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @ApiMessage("User logged in successfully")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(authService.login(request, response));
    }

    @GetMapping("/me")
    @ApiMessage("Current user retrieved successfully")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    @PostMapping("/refresh")
    @ApiMessage("Access token refreshed successfully")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(authService.refresh(request.getCookies(), response));
    }

    @PostMapping("/logout")
    @ApiMessage("User logged out successfully")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request.getCookies(), response);
        return ResponseEntity.ok().build();
    }
}
