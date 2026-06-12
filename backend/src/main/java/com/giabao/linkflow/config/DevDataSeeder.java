package com.giabao.linkflow.config;

import com.giabao.linkflow.user.dto.request.CreateUserRequest;
import com.giabao.linkflow.user.entity.UserRole;
import com.giabao.linkflow.user.repository.UserRepository;
import com.giabao.linkflow.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds known dev accounts on startup so the dashboard is usable immediately.
 * Idempotent (skips existing emails) and disabled in the prod profile.
 */
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {
    private static final String DEFAULT_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void run(String... args) {
        seedUser("admin@gmail.com", "Admin", UserRole.ADMIN);
        seedUser("user@gmail.com", "User", UserRole.USER);
    }

    private void seedUser(String email, String fullName, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        userService.createUser(new CreateUserRequest(email, DEFAULT_PASSWORD, fullName, role));
    }
}
