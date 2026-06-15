package com.giabao.linkflow.user.service.impl;

import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.user.dto.request.CreateUserRequest;
import com.giabao.linkflow.user.dto.request.UpdateUserRequest;
import com.giabao.linkflow.user.dto.request.UserFilterRequest;
import com.giabao.linkflow.user.dto.response.UserResponse;
import com.giabao.linkflow.user.entity.User;
import com.giabao.linkflow.user.mapper.UserMapper;
import com.giabao.linkflow.user.repository.UserRepository;
import com.giabao.linkflow.user.service.UserService;
import com.giabao.linkflow.user.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final String EMAIL_ALREADY_EXISTS_MESSAGE = "Email already exists";
    private static final String USER_PROFILE_NOT_FOUND_MESSAGE = "User profile not found";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUser(UserFilterRequest request) {
        Page<UserResponse> users = userRepository
                .findAll(
                        UserSpecifications.byFilter(request),
                        request.toPageable(UserFilterRequest.ALLOWED_SORT_FIELDS, UserFilterRequest.DEFAULT_SORT_BY)
                )
                .map(userMapper::toResponse);

        return PageResponse.from(users);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userMapper.toResponse(findUserById(id));
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.CONFLICT, EMAIL_ALREADY_EXISTS_MESSAGE);
        }

        User user = userMapper.toEntity(request);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = findUserById(id);

        String email = normalizeEmail(request.email());
        if (!email.equals(user.getEmail()) && userRepository.existsByEmailAndIdNot(email, id)) {
            throw new AppException(ErrorCode.CONFLICT, EMAIL_ALREADY_EXISTS_MESSAGE);
        }

        userMapper.updateEntity(request, user);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new AppException(ErrorCode.NOT_FOUND, USER_PROFILE_NOT_FOUND_MESSAGE);
        }

        userRepository.deleteById(id);
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, USER_PROFILE_NOT_FOUND_MESSAGE));
    }

    private String normalizeEmail(String email) {
        return clean(email).toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
