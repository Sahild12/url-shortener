package com.urlshortener.urlshortener.service;

import java.net.URI;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.urlshortener.urlshortener.dto.UrlResponse;
import com.urlshortener.urlshortener.entity.Url;
import com.urlshortener.urlshortener.repository.UrlRepository;
import com.urlshortener.urlshortener.util.Base62;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public UrlResponse createShortUrl(String longUrl) {
        validate(longUrl);

        Url url = new Url();
        url.setLongUrl(longUrl);
        url.setCreatedAt(LocalDateTime.now());
        url = urlRepository.save(url);          // database generates the id

        url.setShortCode(Base62.encode(url.getId()));
        url = urlRepository.save(url);          // save the short code

        return new UrlResponse(
                url.getShortCode(),
                baseUrl + "/" + url.getShortCode(),
                url.getLongUrl());
    }

    public String getLongUrl(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Short URL not found"));

        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "This link has expired");
        }
        return url.getLongUrl();
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
