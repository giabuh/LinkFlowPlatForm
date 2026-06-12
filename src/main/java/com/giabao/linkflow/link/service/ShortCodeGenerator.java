package com.giabao.linkflow.link.service;

import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.link.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class ShortCodeGenerator {
    private static final String ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 5;
    private static final String GENERATION_FAILED_MESSAGE = "Could not generate a unique short code";

    private final LinkRepository linkRepository;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (!linkRepository.existsByShortCode(code)) {
                return code;
            }
        }

        throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, GENERATION_FAILED_MESSAGE);
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }

        return builder.toString();
    }
}
