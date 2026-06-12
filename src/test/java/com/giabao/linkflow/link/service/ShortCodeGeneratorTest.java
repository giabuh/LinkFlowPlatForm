package com.giabao.linkflow.link.service;

import com.giabao.linkflow.common.exception.AppException;
import com.giabao.linkflow.common.response.ErrorCode;
import com.giabao.linkflow.link.repository.LinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortCodeGeneratorTest {

    @Mock
    private LinkRepository linkRepository;

    @InjectMocks
    private ShortCodeGenerator shortCodeGenerator;

    @Test
    void generatesSevenCharBase62CodeWhenNoCollision() {
        when(linkRepository.existsByShortCode(anyString())).thenReturn(false);

        String code = shortCodeGenerator.generate();

        assertThat(code).hasSize(7).matches("[A-Za-z0-9]{7}");
        verify(linkRepository).existsByShortCode(code);
    }

    @Test
    void retriesOnCollisionThenReturnsUniqueCode() {
        when(linkRepository.existsByShortCode(anyString())).thenReturn(true, true, false);

        String code = shortCodeGenerator.generate();

        assertThat(code).hasSize(7);
        verify(linkRepository, times(3)).existsByShortCode(anyString());
    }

    @Test
    void throwsAfterMaxAttempts() {
        when(linkRepository.existsByShortCode(anyString())).thenReturn(true);

        assertThatThrownBy(() -> shortCodeGenerator.generate())
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR)
                );

        verify(linkRepository, times(5)).existsByShortCode(anyString());
    }
}
