package com.giabao.linkflow.user.service;

import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.user.dto.req.CreateUserRequest;
import com.giabao.linkflow.user.dto.req.UpdateUserRequest;
import com.giabao.linkflow.user.dto.req.UserFilterRequest;
import com.giabao.linkflow.user.dto.res.UserResponse;

import java.util.UUID;

public interface UserService {
    PageResponse<UserResponse> getAllUser(UserFilterRequest request);

    UserResponse getUserById(UUID id);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    void deleteUser(UUID id);
}
