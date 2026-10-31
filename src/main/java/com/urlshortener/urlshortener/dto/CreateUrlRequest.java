package com.urlshortener.urlshortener.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateUrlRequest(
        @NotBlank(message = "longUrl is required")
        String longUrl,

        @Pattern(regexp = "^[a-zA-Z0-9_-]{4,20}$",
                 message = "Alias must be 4-20 characters: letters, numbers, - or _")
        String customAlias,

        @Min(value = 1, message = "expiryDays must be at least 1")
        @Max(value = 365, message = "expiryDays cannot exceed 365")
        Integer expiryDays
) {}