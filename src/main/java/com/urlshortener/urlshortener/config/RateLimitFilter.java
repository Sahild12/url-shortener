package com.urlshortener.urlshortener.config;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;

    @Value("${app.rate-limit.create-per-minute:10}")
    private int createLimit;

    @Value("${app.rate-limit.redirect-per-minute:120}")
    private int redirectLimit;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        String bucket;
        int limit;
        if ("POST".equals(method) && "/api/urls".equals(path)) {
            bucket = "create";
            limit = createLimit;
        } else if ("GET".equals(method) && !path.startsWith("/api") && path.length() > 1 && !path.contains(".")) {
            bucket = "redirect";
            limit = redirectLimit;
        } else {
            chain.doFilter(request, response);
            return;
        }

        long minute = System.currentTimeMillis() / 60_000;
        String key = "rate:" + bucket + ":" + request.getRemoteAddr() + ":" + minute;

        boolean allowed = true;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, Duration.ofSeconds(70));
            }
            if (count != null && count > limit) {
                allowed = false;
            }
        } catch (Exception e) {
            // if Redis is down, let the request through instead of breaking the app
            log.warn("Rate limiter unavailable, allowing request: {}", e.getMessage());
        }

        if (!allowed) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again in a minute.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}