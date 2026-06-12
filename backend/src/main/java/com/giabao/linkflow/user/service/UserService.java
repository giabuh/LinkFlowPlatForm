package com.giabao.linkflow.user.service;

import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.user.dto.request.CreateUserRequest;
import com.giabao.linkflow.user.dto.request.UpdateUserRequest;
import com.giabao.linkflow.user.dto.request.UserFilterRequest;
import com.giabao.linkflow.user.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {
    PageResponse<UserResponse> getAllUser(UserFilterRequest request);

    UserResponse getUserById(UUID id);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    void deleteUser(UUID id);
}
