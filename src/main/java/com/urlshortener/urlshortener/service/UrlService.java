package com.urlshortener.urlshortener.service;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.urlshortener.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.urlshortener.dto.UrlResponse;
import com.urlshortener.urlshortener.entity.Url;
import com.urlshortener.urlshortener.repository.UrlRepository;
import com.urlshortener.urlshortener.util.Base62;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlService {

    private static final Set<String> RESERVED = Set.of("api", "admin", "login", "health");
    private static final String CACHE_PREFIX = "url:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final UrlRepository urlRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request) {
        validate(request.longUrl());

        Url url = new Url();
        url.setLongUrl(request.longUrl());
        url.setCreatedAt(LocalDateTime.now());

        if (request.expiryDays() != null) {
            url.setExpiresAt(LocalDateTime.now().plusDays(request.expiryDays()));
        }

        String alias = request.customAlias();
        if (alias != null) {
            if (RESERVED.contains(alias.toLowerCase()) || urlRepository.existsByShortCode(alias)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Alias already taken");
            }
            url.setShortCode(alias);
            url = urlRepository.save(url);
        } else {
            url = urlRepository.save(url);
            url.setShortCode(Base62.encode(url.getId()));
            url = urlRepository.save(url);
        }

        return new UrlResponse(
                url.getShortCode(),
                baseUrl + "/" + url.getShortCode(),
                url.getLongUrl());
    }

    public String getLongUrl(String shortCode) {
        String key = CACHE_PREFIX + shortCode;

        String cached = readCache(key);
        if (cached != null) {
            log.info("CACHE HIT  -> {}", shortCode);
            return cached;
        }

        log.info("CACHE MISS -> {} (reading database)", shortCode);
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Short URL not found"));

        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "This link has expired");
        }

        writeCache(key, url);
        return url.getLongUrl();
    }

    private String readCache(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis unavailable, falling back to database: {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(String key, Url url) {
        try {
            Duration ttl = DEFAULT_TTL;
            if (url.getExpiresAt() != null) {
                Duration untilExpiry = Duration.between(LocalDateTime.now(), url.getExpiresAt());
                if (untilExpiry.compareTo(ttl) < 0) {
                    ttl = untilExpiry;
                }
            }
            redisTemplate.opsForValue().set(key, url.getLongUrl(), ttl);
        } catch (Exception e) {
            log.warn("Could not write to Redis: {}", e.getMessage());
        }
    }

    private void validate(String longUrl) {
        try {
            URI uri = URI.create(longUrl);
            String scheme = uri.getScheme();
            boolean validScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            if (!validScheme || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please enter a valid URL starting with http:// or https://");
        }
    }
}
 