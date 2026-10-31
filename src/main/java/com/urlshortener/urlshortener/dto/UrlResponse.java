package com.urlshortener.urlshortener.dto;

public record UrlResponse(
        String shortCode,
        String shortUrl,
        String longUrl
) {}