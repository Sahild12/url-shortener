package com.urlshortener.urlshortener.dto;

import java.util.Map;

public record StatsResponse(
        String shortCode,
        long totalClicks,
        Map<String, Long> byBrowser,
        Map<String, Long> byDevice
) {}