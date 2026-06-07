package com.giabao.linkflow.common.response;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void storesHttpStatusAsTheSingleCodeSource() {
        assertThat(Arrays.stream(ErrorCode.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("code");
    }

    @Test
    void getCodeReturnsHttpStatusValue() {
        assertThat(ErrorCode.SUCCESS.getCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(ErrorCode.INVALID_ARGUMENT.getCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(ErrorCode.NOT_FOUND.getCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(ErrorCode.CONFLICT.getCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void usesGenericHttpErrorNames() {
        assertThat(Stream.of(ErrorCode.values()).map(Enum::name))
                .contains("NOT_FOUND", "CONFLICT")
                .doesNotContain("USER_NOT_FOUND", "USER_EMAIL_ALREADY_EXISTS");
    }
}
