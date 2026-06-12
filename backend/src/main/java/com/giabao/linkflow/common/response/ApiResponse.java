package com.giabao.linkflow.common.response;

public record ApiResponse<T>(
        int code,
        String message,
        T data
) {
}
