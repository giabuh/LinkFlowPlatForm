package com.giabao.linkflow.common.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    SUCCESS(HttpStatus.OK, "API called successfully"),
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "Validation failed for specified fields"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Missing or invalid authentication token"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    CONFLICT(HttpStatus.CONFLICT, "Resource conflict"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred on the server");

    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return httpStatus.value();
    }

    public static ErrorCode fromHttpStatus(int statusCode) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.httpStatus.value() == statusCode) {
                return errorCode;
            }
        }

        return statusCode >= HttpStatus.BAD_REQUEST.value() ? INTERNAL_SERVER_ERROR : SUCCESS;
    }
}
