package com.satish.urlshortener.service;

import com.satish.urlshortener.config.ShortenerProperties;
import com.satish.urlshortener.exception.InvalidUrlException;
import com.satish.urlshortener.exception.ShortCodeGenerationException;
import com.satish.urlshortener.exception.UrlNotFoundException;
import com.satish.urlshortener.model.UrlMapping;
import com.satish.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for UrlService.
 * Repository and generator are mocks (fakes),
 * so we control their answers and test only the service logic.
 */
@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlMappingRepository repository;

    @Mock
    private ShortCodeGenerator codeGenerator;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        ShortenerProperties properties =
                new ShortenerProperties("http://localhost:8080", 7, 3);
        urlService = new UrlService(repository, codeGenerator, properties);
    }

    @Test
    void shortenSavesAndReturnsMapping() {
        when(codeGenerator.generate()).thenReturn("abc1234");
        when(repository.existsByShortCode("abc1234")).thenReturn(false);
        when(repository.saveAndFlush(any(UrlMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UrlMapping result = urlService.shorten("https://example.com/page");

        assertThat(result.getShortCode()).isEqualTo("abc1234");
        assertThat(result.getOriginalUrl()).isEqualTo("https://example.com/page");
    }

    @Test
    void shortenRejectsEmptyUrl() {
        assertThatThrownBy(() -> urlService.shorten(""))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void shortenRejectsNonHttpUrl() {
        assertThatThrownBy(() -> urlService.shorten("ftp://example.com/file"))
                .isInstanceOf(InvalidUrlException.class);

        assertThatThrownBy(() -> urlService.shorten("javascript:alert(1)"))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void shortenRejectsTooLongUrl() {
        String longUrl = "https://example.com/" + "a".repeat(3000);

        assertThatThrownBy(() -> urlService.shorten(longUrl))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void shortenRejectsUrlWithoutHost() {
        assertThatThrownBy(() -> urlService.shorten("https://"))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void shortenRetriesWhenCodeAlreadyExists() {
        // First code is taken, second is free
        when(codeGenerator.generate()).thenReturn("AAAAAAA", "BBBBBBB");
        when(repository.existsByShortCode("AAAAAAA")).thenReturn(true);
        when(repository.existsByShortCode("BBBBBBB")).thenReturn(false);
        when(repository.saveAndFlush(any(UrlMapping.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UrlMapping result = urlService.shorten("https://example.com");

        assertThat(result.getShortCode()).isEqualTo("BBBBBBB");
    }

    @Test
    void shortenFailsAfterAllRetries() {
        // Every generated code is already taken
        when(codeGenerator.generate()).thenReturn("AAAAAAA");
        when(repository.existsByShortCode("AAAAAAA")).thenReturn(true);

        assertThatThrownBy(() -> urlService.shorten("https://example.com"))
                .isInstanceOf(ShortCodeGenerationException.class);

        // Save was never called because no free code was found
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void resolveReturnsMappingWhenCodeExists() {
        UrlMapping mapping = new UrlMapping("abc1234", "https://example.com", Instant.now());
        when(repository.findByShortCode("abc1234")).thenReturn(Optional.of(mapping));

        UrlMapping result = urlService.resolve("abc1234");

        assertThat(result.getOriginalUrl()).isEqualTo("https://example.com");
    }

    @Test
    void resolveThrowsWhenCodeUnknown() {
        when(repository.findByShortCode("nothere")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.resolve("nothere"))
                .isInstanceOf(UrlNotFoundException.class);
    }

    @Test
    void buildShortUrlJoinsBaseUrlAndCode() {
        String result = urlService.buildShortUrl("abc1234");

        assertThat(result).isEqualTo("http://localhost:8080/abc1234");
    }
}