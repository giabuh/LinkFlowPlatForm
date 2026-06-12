package com.giabao.linkflow.link.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

public record CreateLinkRequest(
        @NotBlank
        @URL
        String originalUrl,

        @Size(max = 255)
        String title,

        @Future
        Instant expiresAt
) {
}
