package com.urlshortener.urlshortener.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.urlshortener.urlshortener.dto.StatsResponse;
import com.urlshortener.urlshortener.entity.ClickEvent;
import com.urlshortener.urlshortener.repository.ClickEventRepository;
import com.urlshortener.urlshortener.repository.UrlRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickService {

    private final ClickEventRepository clickRepository;
    private final UrlRepository urlRepository;

    @Async
    public void record(String shortCode, String userAgent) {
        try {
            ClickEvent event = new ClickEvent();
            event.setShortCode(shortCode);
            event.setClickedAt(LocalDateTime.now());
            event.setBrowser(detectBrowser(userAgent));
            event.setDevice(detectDevice(userAgent));
            clickRepository.save(event);
        } catch (Exception e) {
            log.warn("Could not record click for {}: {}", shortCode, e.getMessage());
        }
    }

    public StatsResponse getStats(String shortCode) {
        if (!urlRepository.existsByShortCode(shortCode)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Short URL not found");
        }
        return new StatsResponse(
                shortCode,
                clickRepository.countByShortCode(shortCode),
                toMap(clickRepository.countByBrowser(shortCode)),
                toMap(clickRepository.countByDevice(shortCode)));
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return map;
    }

    private String detectBrowser(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("PostmanRuntime")) return "Postman";
        if (ua.contains("curl")) return "curl";
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera")) return "Opera";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Safari")) return "Safari";
        return "Other";
    }

    private String detectDevice(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("iPad") || ua.contains("Tablet")) return "Tablet";
        if (ua.contains("Mobi") || ua.contains("Android") || ua.contains("iPhone")) return "Mobile";
        return "Desktop";
    }
}