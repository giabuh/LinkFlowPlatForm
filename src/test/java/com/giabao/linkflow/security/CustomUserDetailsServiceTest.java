package com.giabao.linkflow.security;

import com.giabao.linkflow.user.entity.User;
import com.giabao.linkflow.user.entity.UserRole;
import com.giabao.linkflow.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsernameReturnsCustomPrincipalWithRoleAuthority() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "ada@example.com", "hash", UserRole.ADMIN);
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

        CustomUserDetails details = (CustomUserDetails) service.loadUserByUsername("ada@example.com");

        assertThat(details.getId()).isEqualTo(userId);
        assertThat(details.getUsername()).isEqualTo("ada@example.com");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByIdReturnsCustomPrincipal() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "ada@example.com", "hash", UserRole.USER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        CustomUserDetails details = service.loadUserById(userId);

        assertThat(details.getId()).isEqualTo(userId);
        assertThat(details.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void loadUserByUsernameThrowsWhenMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

    private User user(UUID id, String email, String password, UserRole role) throws Exception {
        User user = new User();
        setId(user, id);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setFullName("Ada Lovelace");
        return user;
    }

    private void setId(User user, UUID id) throws Exception {
        Field field = user.getClass().getSuperclass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}
