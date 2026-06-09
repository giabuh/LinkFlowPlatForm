package com.giabao.linkflow.user.service;

import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.user.dto.request.CreateUserRequest;
import com.giabao.linkflow.user.dto.request.UpdateUserRequest;
import com.giabao.linkflow.user.dto.request.UserFilterRequest;
import com.giabao.linkflow.user.dto.response.UserResponse;
import com.giabao.linkflow.user.entity.User;
import com.giabao.linkflow.user.entity.UserRole;
import com.giabao.linkflow.user.mapper.UserMapper;
import com.giabao.linkflow.user.repository.UserRepository;
import com.giabao.linkflow.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getAllUserUsesSpecificationAndPageable() {
        UserFilterRequest request = new UserFilterRequest();
        request.setPage(1);
        request.setSize(10);
        request.setSortBy("email");
        request.setSortDirection("asc");
        request.setEmail("ada");
        request.setRole(UserRole.ADMIN);

        User user = new User();
        user.setEmail("ada@example.com");
        user.setFullName("Ada Lovelace");
        user.setRole(UserRole.ADMIN);
        UserResponse response = new UserResponse(UUID.randomUUID(), "ada@example.com", "Ada Lovelace", UserRole.ADMIN);

        when(userRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(user),
                        request.toPageable(UserFilterRequest.ALLOWED_SORT_FIELDS, UserFilterRequest.DEFAULT_SORT_BY),
                        21
                ));
        when(userMapper.toResponse(user)).thenReturn(response);

        PageResponse<UserResponse> actual = userService.getAllUser(request);

        assertThat(actual.content()).containsExactly(response);
        assertThat(actual.page()).isEqualTo(1);
        assertThat(actual.size()).isEqualTo(10);
        assertThat(actual.totalElements()).isEqualTo(21);
        assertThat(actual.totalPages()).isEqualTo(3);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(anySpecification(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "email"));
    }

    private Specification<User> anySpecification() {
        return any();
    }

    @Test
    void createUserEncodesPasswordAndStoresRole() {
        CreateUserRequest request = new CreateUserRequest(
                "ada@example.com",
                "secret-password",
                "Ada Lovelace",
                UserRole.ADMIN
        );
        User mappedUser = new User();
        mappedUser.setFullName("Ada Lovelace");
        mappedUser.setRole(UserRole.ADMIN);
        UserResponse response = new UserResponse(UUID.randomUUID(), "ada@example.com", "Ada Lovelace", UserRole.ADMIN);

        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode("secret-password")).thenReturn("hashed-password");
        when(userRepository.save(mappedUser)).thenReturn(mappedUser);
        when(userMapper.toResponse(any(User.class))).thenReturn(response);

        UserResponse actual = userService.createUser(request);

        assertThat(actual).isSameAs(response);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser).isSameAs(mappedUser);
        assertThat(savedUser.getEmail()).isEqualTo("ada@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("hashed-password");
        assertThat(savedUser.getFullName()).isEqualTo("Ada Lovelace");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userMapper).toEntity(request);
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        CreateUserRequest request = new CreateUserRequest(
                "ada@example.com",
                "secret-password",
                "Ada Lovelace",
                UserRole.USER
        );

        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT)
                )
                .hasMessage("Email already exists");

        verify(userRepository).existsByEmail("ada@example.com");
        verifyNoMoreInteractions(userRepository, passwordEncoder, userMapper);
    }

    @Test
    void updateUserChangesFieldsAndEncodesNewPassword() {
        UUID userId = UUID.randomUUID();
        User existingUser = new User();
        existingUser.setEmail("old@example.com");
        existingUser.setPassword("old-hash");
        existingUser.setFullName("Old Name");
        existingUser.setRole(UserRole.USER);
        UpdateUserRequest request = new UpdateUserRequest(
                "new@example.com",
                "new-secret-password",
                "New Name",
                UserRole.ADMIN
        );
        UserResponse response = new UserResponse(userId, "new@example.com", "New Name", UserRole.ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmailAndIdNot("new@example.com", userId)).thenReturn(false);
        doAnswer(invocation -> {
            UpdateUserRequest source = invocation.getArgument(0);
            User target = invocation.getArgument(1);
            target.setFullName(source.fullName());
            target.setRole(source.role());
            return null;
        }).when(userMapper).updateEntity(request, existingUser);
        when(passwordEncoder.encode("new-secret-password")).thenReturn("new-hash");
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.toResponse(existingUser)).thenReturn(response);

        UserResponse actual = userService.updateUser(userId, request);

        assertThat(actual).isSameAs(response);
        assertThat(existingUser.getEmail()).isEqualTo("new@example.com");
        assertThat(existingUser.getPassword()).isEqualTo("new-hash");
        assertThat(existingUser.getFullName()).isEqualTo("New Name");
        assertThat(existingUser.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userMapper).updateEntity(request, existingUser);
    }

    @Test
    void updateUserRejectsEmailOwnedByAnotherUser() {
        UUID userId = UUID.randomUUID();
        User existingUser = new User();
        existingUser.setEmail("old@example.com");
        UpdateUserRequest request = new UpdateUserRequest(
                "taken@example.com",
                "new-secret-password",
                "New Name",
                UserRole.ADMIN
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmailAndIdNot("taken@example.com", userId)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(userId, request))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT)
                )
                .hasMessage("Email already exists");
    }

    @Test
    void getUserByIdThrowsWhenUserIsMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND)
                )
                .hasMessage("User profile not found");
    }

    @Test
    void deleteUserThrowsWhenUserIsMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND)
                )
                .hasMessage("User profile not found");
    }
}
