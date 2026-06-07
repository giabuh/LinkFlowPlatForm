package com.giabao.linkflow.user.dto.res;


import com.giabao.linkflow.user.enity.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        UserRole role
){

}
