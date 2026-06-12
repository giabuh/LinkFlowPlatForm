package com.giabao.linkflow.common.exception;

import com.giabao.linkflow.common.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.giabao.linkflow.common.response.ApiResponseBodyAdvice.ERROR_CODE_ATTRIBUTE;
import static com.giabao.linkflow.common.response.ApiResponseBodyAdvice.ERROR_MESSAGE_ATTRIBUTE;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Object> handleAppException(AppException exception, HttpServletRequest request) {
        return response(exception.getErrorCode(), exception.getMessage(), null, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return response(ErrorCode.INVALID_ARGUMENT, errors, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(exception.getName(), "Invalid value");

        return response(ErrorCode.INVALID_ARGUMENT, errors, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return response(ErrorCode.INVALID_ARGUMENT, null, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
        return response(ErrorCode.UNAUTHORIZED, null, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleInternalServerError(Exception exception, HttpServletRequest request) {
        return response(ErrorCode.INTERNAL_SERVER_ERROR, null, request);
    }

    private ResponseEntity<Object> response(ErrorCode errorCode, Object data, HttpServletRequest request) {
        return response(errorCode, errorCode.getMessage(), data, request);
    }

    private ResponseEntity<Object> response(
            ErrorCode errorCode,
            String message,
            Object data,
            HttpServletRequest request
    ) {
        request.setAttribute(ERROR_CODE_ATTRIBUTE, errorCode);
        request.setAttribute(ERROR_MESSAGE_ATTRIBUTE, message);

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(data == null ? errorCode : data);
    }
}
