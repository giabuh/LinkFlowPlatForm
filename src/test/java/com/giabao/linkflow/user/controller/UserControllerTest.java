package com.giabao.linkflow.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giabao.linkflow.common.dto.PageResponse;
import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.auth.service.TokenService;
import com.giabao.linkflow.auth.service.RefreshSessionService;
import com.giabao.linkflow.security.CustomUserDetailsService;
import com.giabao.linkflow.user.dto.request.CreateUserRequest;
import com.giabao.linkflow.user.dto.request.UpdateUserRequest;
import com.giabao.linkflow.user.dto.request.UserFilterRequest;
import com.giabao.linkflow.user.dto.response.UserResponse;
import com.giabao.linkflow.user.entity.UserRole;
import com.giabao.linkflow.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private RefreshSessionService refreshSessionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getAllUsersReturnsPagedUsersWithFilters() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getAllUser(any(UserFilterRequest.class))).thenReturn(new PageResponse<>(
                1,
                5,
                3,
                12,
                List.of(new UserResponse(userId, "ada@example.com", "Ada Lovelace", UserRole.ADMIN))
        ));

        mockMvc.perform(get("/users")
                        .param("page", "1")
                        .param("size", "5")
                        .param("email", "ada")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].id").value(userId.toString()))
                .andExpect(jsonPath("$.data.content[0].email").value("ada@example.com"))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.data.content[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(12))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.last").doesNotExist());

        ArgumentCaptor<UserFilterRequest> requestCaptor = ArgumentCaptor.forClass(UserFilterRequest.class);
        verify(userService).getAllUser(requestCaptor.capture());
        UserFilterRequest request = requestCaptor.getValue();
        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getSize()).isEqualTo(5);
        assertThat(request.getEmail()).isEqualTo("ada");
        assertThat(request.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void getUserByIdReturnsUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getUserById(userId))
                .thenReturn(new UserResponse(userId, "ada@example.com", "Ada Lovelace", UserRole.USER));

        mockMvc.perform(get("/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("ada@example.com"));
    }

    @Test
    void createUserReturnsCreatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateUserRequest request = new CreateUserRequest(
                "ada@example.com",
                "secret-password",
                "Ada Lovelace",
                UserRole.ADMIN
        );
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenReturn(new UserResponse(userId, "ada@example.com", "Ada Lovelace", UserRole.ADMIN));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("ada@example.com"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        verify(userService).createUser(any(CreateUserRequest.class));
    }

    @Test
    void createUserRejectsInvalidRequest() throws Exception {
        CreateUserRequest request = new CreateUserRequest("not-an-email", "", "", UserRole.USER);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed for specified fields"))
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.password").exists())
                .andExpect(jsonPath("$.data.fullName").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void createUserRejectsNullRole() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "ada@example.com",
                "secret-password",
                "Ada Lovelace",
                null
        );

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed for specified fields"))
                .andExpect(jsonPath("$.data.role").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void createUserRejectsDuplicateEmail() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "ada@example.com",
                "secret-password",
                "Ada Lovelace",
                UserRole.USER
        );
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new AppException(ErrorCode.CONFLICT, "Email already exists"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void invalidUserIdReturnsInvalidArgumentEnvelope() throws Exception {
        mockMvc.perform(get("/users/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed for specified fields"))
                .andExpect(jsonPath("$.data.id").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void invalidUserFilterReturnsInvalidArgumentEnvelope() throws Exception {
        mockMvc.perform(get("/users")
                        .param("role", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed for specified fields"))
                .andExpect(jsonPath("$.data.role").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void updateUserReturnsUpdatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest(
                "grace@example.com",
                "new-secret-password",
                "Grace Hopper",
                UserRole.ADMIN
        );
        when(userService.updateUser(userId, request))
                .thenReturn(new UserResponse(userId, "grace@example.com", "Grace Hopper", UserRole.ADMIN));

        mockMvc.perform(put("/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User updated successfully"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("grace@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Grace Hopper"));
    }

    @Test
    void updateUserRejectsInvalidRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest("", "", "", null);

        mockMvc.perform(put("/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed for specified fields"))
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.password").exists())
                .andExpect(jsonPath("$.data.fullName").exists())
                .andExpect(jsonPath("$.data.role").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void deleteUserReturnsSuccessEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(userService).deleteUser(userId);
    }

    @Test
    void missingUserReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        doThrow(new AppException(ErrorCode.NOT_FOUND, "User profile not found")).when(userService).deleteUser(userId);

        mockMvc.perform(delete("/users/{id}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("User profile not found"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
