package com.giabao.linkflow.common.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes a JSON {@link ApiResponse} error envelope directly to the servlet response.
 * Shared by the Spring Security entry point and access-denied handler, which run
 * outside the MVC dispatcher and therefore cannot rely on {@code ApiResponseBodyAdvice}.
 */
@Component
@RequiredArgsConstructor
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ApiResponse<>(
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        ));
    }
}
