package com.giabao.linkflow.user.dto.response;


import com.giabao.linkflow.user.entity.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        UserRole role
){

}
