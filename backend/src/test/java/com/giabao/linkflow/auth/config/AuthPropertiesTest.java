package com.giabao.linkflow.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "app.jwt.secret=12345678901234567890123456789012",
                    "app.jwt.access-token-expiration-ms=900000",
                    "app.jwt.refresh-token-expiration-ms=604800000",
                    "app.auth.refresh-cookie.name=refresh_token",
                    "app.auth.refresh-cookie.http-only=true",
                    "app.auth.refresh-cookie.secure=false",
                    "app.auth.refresh-cookie.same-site=Lax",
                    "app.auth.refresh-cookie.path=/api/v1/auth/refresh"
            );

    @Test
    void bindsJwtAndRefreshCookieProperties() {
        contextRunner.run(context -> {
            JwtProperties jwt = context.getBean(JwtProperties.class);
            RefreshCookieProperties cookie = context.getBean(RefreshCookieProperties.class);

            assertThat(jwt.secret()).isEqualTo("12345678901234567890123456789012");
            assertThat(jwt.accessTokenExpirationMs()).isEqualTo(900000);
            assertThat(jwt.refreshTokenExpirationMs()).isEqualTo(604800000);
            assertThat(cookie.name()).isEqualTo("refresh_token");
            assertThat(cookie.httpOnly()).isTrue();
            assertThat(cookie.secure()).isFalse();
            assertThat(cookie.sameSite()).isEqualTo("Lax");
            assertThat(cookie.path()).isEqualTo("/api/v1/auth/refresh");
        });
    }

    @EnableConfigurationProperties({JwtProperties.class, RefreshCookieProperties.class})
    static class TestConfig {
    }
}
