package com.urlshortener.urlshortener.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.urlshortener.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.urlshortener.dto.StatsResponse;
import com.urlshortener.urlshortener.dto.UrlResponse;
import com.urlshortener.urlshortener.service.ClickService;
import com.urlshortener.urlshortener.service.UrlService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final ClickService clickService;

    @PostMapping("/api/urls")
    public ResponseEntity<UrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        UrlResponse response = urlService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/urls/{shortCode}/stats")
    public StatsResponse stats(@PathVariable String shortCode) {
        return clickService.getStats(shortCode);
    }

    @GetMapping("/{shortCode:[a-zA-Z0-9_-]+}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode,
                                         HttpServletRequest request) {
        String longUrl = urlService.getLongUrl(shortCode);
        clickService.record(shortCode, request.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }
}