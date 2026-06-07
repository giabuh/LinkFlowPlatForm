package com.giabao.linkflow.common.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giabao.linkflow.common.annotation.ApiMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {
    public static final String ERROR_CODE_ATTRIBUTE = "apiResponse.errorCode";
    public static final String ERROR_MESSAGE_ATTRIBUTE = "apiResponse.errorMessage";

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (body instanceof ApiResponse<?>) {
            return body;
        }

        int statusCode = getStatusCode(response);
        ErrorCode errorCode = resolveErrorCode(request, statusCode, body);
        Object data = body instanceof ErrorCode ? null : body;
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                errorCode.getCode(),
                resolveMessage(returnType, request, errorCode),
                data
        );

        if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return writeAsJson(apiResponse);
        }

        return apiResponse;
    }

    private int getStatusCode(ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse servletResponse) {
            return servletResponse.getServletResponse().getStatus();
        }

        return ErrorCode.SUCCESS.getCode();
    }

    private ErrorCode resolveErrorCode(ServerHttpRequest request, int statusCode, Object body) {
        if (body instanceof ErrorCode errorCode) {
            return errorCode;
        }

        if (statusCode < ErrorCode.INVALID_ARGUMENT.getCode()) {
            return ErrorCode.SUCCESS;
        }

        Object errorCode = getRequestAttribute(request, ERROR_CODE_ATTRIBUTE);
        if (errorCode instanceof ErrorCode value) {
            return value;
        }

        return ErrorCode.fromHttpStatus(statusCode);
    }

    private String resolveMessage(MethodParameter returnType, ServerHttpRequest request, ErrorCode errorCode) {
        Object message = getRequestAttribute(request, ERROR_MESSAGE_ATTRIBUTE);
        if (message instanceof String value && !value.isBlank()) {
            return value;
        }

        if (errorCode == ErrorCode.SUCCESS) {
            return resolveSuccessMessage(returnType);
        }

        return errorCode.getMessage();
    }

    private String resolveSuccessMessage(MethodParameter returnType) {
        ApiMessage apiMessage = returnType.getMethodAnnotation(ApiMessage.class);
        if (apiMessage == null) {
            return ErrorCode.SUCCESS.getMessage();
        }

        return apiMessage.value();
    }

    private Object getRequestAttribute(ServerHttpRequest request, String name) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            return servletRequest.getServletRequest().getAttribute(name);
        }

        return null;
    }

    private String writeAsJson(ApiResponse<Object> apiResponse) {
        try {
            return objectMapper.writeValueAsString(apiResponse);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write API response", exception);
        }
    }
}
