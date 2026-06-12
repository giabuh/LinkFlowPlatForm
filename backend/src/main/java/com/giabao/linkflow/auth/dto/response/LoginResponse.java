package com.giabao.linkflow.auth.dto.response;

import com.giabao.linkflow.user.dto.response.UserResponse;

public record LoginResponse(String accessToken, UserResponse user) {
}
