package com.example.urlshortener.url.service;


import com.example.urlshortener.model.User;
import com.example.urlshortener.url.model.ShortUrl;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

 class UrlShortenerServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UrlShortenerService urlShortenerService;

    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cacheManager = new ConcurrentMapCacheManager();
    }

    @Test
    void createShortUrl() {
        User user = new User(); // Створіть об'єкт User з потрібними даними
        String originalUrl = "http://example.com";
        Duration activeDuration = Duration.ofDays(1);
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setShortUrl("shortUrl");
        shortUrl.setOriginalUrl(originalUrl);
        shortUrl.setCreatedAt(LocalDateTime.now());
        shortUrl.setExpiryDate(shortUrl.getCreatedAt().plus(activeDuration));
        shortUrl.setCreatedBy(user);

        when(shortUrlRepository.save(any(ShortUrl.class))).thenReturn(shortUrl);

        ShortUrl createdShortUrl = urlShortenerService.createShortUrl(originalUrl, user, activeDuration);

        assertNotNull(createdShortUrl);
        assertEquals(originalUrl, createdShortUrl.getOriginalUrl());
        assertEquals("shortUrl", createdShortUrl.getShortUrl());
        verify(shortUrlRepository, times(1)).save(any(ShortUrl.class));
    }

    @Test
    void getActiveUrls() {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setExpiryDate(LocalDateTime.now().plusDays(1));
        when(shortUrlRepository.findByExpiryDateAfter(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(shortUrl));

        List<ShortUrl> activeUrls = urlShortenerService.getActiveUrls();

        assertNotNull(activeUrls);
        assertFalse(activeUrls.isEmpty());
        assertEquals(1, activeUrls.size());
        assertEquals(shortUrl, activeUrls.get(0));
    }

    @Test
    void getOriginalUrl() {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setShortUrl("shortUrl");
        when(shortUrlRepository.findByShortUrl(anyString())).thenReturn(Optional.of(shortUrl));

        Optional<ShortUrl> foundUrl = urlShortenerService.getOriginalUrl("shortUrl");

        assertTrue(foundUrl.isPresent());
        assertEquals("shortUrl", foundUrl.get().getShortUrl());
    }

    @Test
    void getUrlsByUser() {
        User user = new User(); // Створіть об'єкт User з потрібними даними
        ShortUrl shortUrl = new ShortUrl();
        when(shortUrlRepository.findByCreatedBy(any(User.class)))
                .thenReturn(Collections.singletonList(shortUrl));

        List<ShortUrl> userUrls = urlShortenerService.getUrlsByUser(user);

        assertNotNull(userUrls);
        assertFalse(userUrls.isEmpty());
        assertEquals(shortUrl, userUrls.get(0));
    }

    @Test
    void incrementVisitCount() {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setVisitCount(5);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenReturn(shortUrl);

        urlShortenerService.incrementVisitCount(shortUrl);

        assertEquals(6, shortUrl.getVisitCount());
        verify(shortUrlRepository, times(1)).save(shortUrl);
    }

    @Test
    void deleteURL() {
        User user = new User(); // Створіть об'єкт User з потрібними даними
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setCreatedBy(user);

        when(shortUrlRepository.existsByShortUrl(anyString())).thenReturn(true);

        urlShortenerService.deleteURL(shortUrl, user);

        verify(shortUrlRepository, times(1)).delete(shortUrl);
    }

    @Test
    void generateShortUrl() {
        String shortUrl = urlShortenerService.generateShortUrl();

        assertNotNull(shortUrl);
        assertEquals(8, shortUrl.length());
    }
}
/*
import com.example.urlshortener.model.User;
import com.example.urlshortener.repository.UserRepository;
import com.example.urlshortener.url.model.ShortUrl;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
/*
@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UrlShortenerService urlShortenerService;

    private User testUser;
    private ShortUrl testShortUrl;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testShortUrl = new ShortUrl();
        testShortUrl.setOriginalUrl("https://example.com");
        testShortUrl.setShortUrl("abc123");
        testShortUrl.setCreatedAt(LocalDateTime.now());
        testShortUrl.setExpiryDate(Duration.ofMinutes(5));
        testShortUrl.setCreatedBy(testUser);
    }

    @Test
    void createShortUrl_ShouldReturnShortUrl() {
        when(shortUrlRepository.existsByShortUrl(anyString())).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenReturn(testShortUrl);

        ShortUrl result = urlShortenerService.createShortUrl(testShortUrl.getOriginalUrl(), testUser, testShortUrl.getExpiryDate());

        assertNotNull(result);
        assertEquals(testShortUrl.getOriginalUrl(), result.getOriginalUrl());
        assertEquals(testShortUrl.getCreatedBy(), result.getCreatedBy());
        verify(shortUrlRepository, times(1)).save(any(ShortUrl.class));
    }

    @Test
    void getActiveUrls_ShouldReturnListOfUrls() {
        when(shortUrlRepository.findByExpiryDateAfter(any(LocalDateTime.class))).thenReturn(List.of(testShortUrl));

        List<ShortUrl> result = urlShortenerService.getActiveUrls();

        assertFalse(result.isEmpty());
        assertEquals(testShortUrl, result.get(0));
        verify(shortUrlRepository, times(1)).findByExpiryDateAfter(any(LocalDateTime.class));
    }

    @Test
    void getOriginalUrl_ShouldReturnOptionalShortUrl() {
        when(shortUrlRepository.findByShortUrl(anyString())).thenReturn(Optional.of(testShortUrl));

        Optional<ShortUrl> result = urlShortenerService.getOriginalUrl("abc123");

        assertTrue(result.isPresent());
        assertEquals(testShortUrl, result.get());
        verify(shortUrlRepository, times(1)).findByShortUrl(anyString());
    }

    @Test
    void getUrlsByUser_ShouldReturnListOfUrls() {
        when(shortUrlRepository.findByCreatedBy(any(User.class))).thenReturn(List.of(testShortUrl));

        List<ShortUrl> result = urlShortenerService.getUrlsByUser(testUser);

        assertFalse(result.isEmpty());
        assertEquals(testShortUrl, result.get(0));
        verify(shortUrlRepository, times(1)).findByCreatedBy(any(User.class));
    }

    @Test
    void isUrlExpired_ShouldReturnTrueIfExpired() {
        ShortUrl expiredUrl = new ShortUrl();
        expiredUrl.setExpiryDate(LocalDateTime.now().minusDays(1));

        boolean result = urlShortenerService.isUrlExpired(expiredUrl);

        assertTrue(result);
    }

    @Test
    void isUrlExpired_ShouldReturnFalseIfNotExpired() {
        boolean result = urlShortenerService.isUrlExpired(testShortUrl);

        assertFalse(result);
    }

    @Test
    void incrementVisitCount_ShouldIncreaseVisitCount() {
        int initialVisitCount = testShortUrl.getVisitCount();

        urlShortenerService.incrementVisitCount(testShortUrl);

        assertEquals(initialVisitCount + 1, testShortUrl.getVisitCount());
        verify(shortUrlRepository, times(1)).save(testShortUrl);
    }
    @Test
    void deleteURL_ShouldDeleteUrlIfUserIsOwner() {
        // Видаляємо непотрібний мок
        // when(shortUrlRepository.findByShortUrl(anyString())).thenReturn(Optional.of(testShortUrl));

        // Виклик методу, який тестується
        urlShortenerService.deleteURL(testShortUrl, testUser);

        // Перевірка, що метод видалення було викликано
        verify(shortUrlRepository, times(1)).delete(testShortUrl);
    }


    @Test
    void deleteURL_ShouldThrowExceptionIfUserIsNotOwner() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setEmail("another@example.com");

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                urlShortenerService.deleteURL(testShortUrl, anotherUser)
        );

        assertEquals("User not authorized to delete this URL", exception.getMessage());
        verify(shortUrlRepository, never()).delete(any(ShortUrl.class));
    }

    @Test
    void generateShortUrl_ShouldReturnUniqueShortUrl() {
        when(shortUrlRepository.existsByShortUrl(anyString())).thenReturn(false);

        String shortUrl = urlShortenerService.generateShortUrl();

        assertNotNull(shortUrl);
        assertEquals(8, shortUrl.length());
        verify(shortUrlRepository, times(1)).existsByShortUrl(anyString());
    }

    @Test
    void generateShortUrl_ShouldRegenerateIfNotUnique() {
        when(shortUrlRepository.existsByShortUrl(anyString())).thenReturn(true, false);

        String shortUrl = urlShortenerService.generateShortUrl();

        assertNotNull(shortUrl);
        assertEquals(8, shortUrl.length());
        verify(shortUrlRepository, times(2)).existsByShortUrl(anyString());
    }
}
*/