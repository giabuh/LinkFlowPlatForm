package com.giabao.linkflow.auth.service;

import com.giabao.linkflow.auth.config.JwtProperties;
import com.giabao.linkflow.auth.config.RefreshCookieProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshCookieServiceTest {

    private final RefreshCookieService service = new RefreshCookieService(
            new RefreshCookieProperties("refresh_token", true, false, "Lax", "/api/v1/auth/refresh"),
            new JwtProperties("12345678901234567890123456789012", 900000, 604800000)
    );

    @Test
    void addsRefreshCookieWithConfiguredAttributes() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.addRefreshCookie(response, "refresh-token-value");

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains("refresh_token=refresh-token-value");
        assertThat(setCookie).contains("Path=/api/v1/auth/refresh");
        assertThat(setCookie).contains("Max-Age=604800");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=Lax");
    }

    @Test
    void clearsRefreshCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.clearRefreshCookie(response);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains("refresh_token=");
        assertThat(setCookie).contains("Max-Age=0");
        assertThat(setCookie).contains("Path=/api/v1/auth/refresh");
    }

    @Test
    void extractsRefreshCookieValue() {
        Cookie[] cookies = {new Cookie("refresh_token", "abc")};

        assertThat(service.extractRefreshToken(cookies)).contains("abc");
    }
}
