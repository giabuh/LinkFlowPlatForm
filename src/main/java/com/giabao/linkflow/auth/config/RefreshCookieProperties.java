package com.giabao.linkflow.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.refresh-cookie")
public record RefreshCookieProperties(
        String name,
        boolean httpOnly,
        boolean secure,
        String sameSite,
        String path
) {
}
