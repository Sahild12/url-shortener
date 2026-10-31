package com.urlshortener.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUrlRequest(
        @NotBlank(message = "longUrl is required")
        String longUrl
) {}