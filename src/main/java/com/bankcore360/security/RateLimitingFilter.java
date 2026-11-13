package com.bankcore360.security;

import com.bankcore360.exception.ErrorResponse;
import com.bankcore360.observability.FintechMetricsService;
import com.bankcore360.service.SessionManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-Throughput Redis-backed Distributed Rate Limiting Filter.
 * Protects login, MFA endpoints, and sensitive financial ledger transaction APIs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final FintechMetricsService fintechMetricsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Fallback in-memory counters if Redis is offline
    private final Map<String, AtomicInteger> localCounters = new ConcurrentHashMap<>();
    private final Map<String, Long> localExpiries = new ConcurrentHashMap<>();

    public RateLimitingFilter(@org.springframework.beans.factory.annotation.Autowired(required = false)
                              StringRedisTemplate stringRedisTemplate,
                              @org.springframework.beans.factory.annotation.Autowired(required = false)
                              FintechMetricsService fintechMetricsService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.fintechMetricsService = fintechMetricsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 1. Determine rate limit rule for endpoint
        RateLimitRule rule = resolveRule(uri, method);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = SessionManagementService.extractClientIp(request);
        String clientKey = "rl:" + rule.name + ":" + clientIp;

        // 2. Increment and check rate limit counter
        long currentCount = incrementAndGet(clientKey, rule.windowSeconds);
        long remaining = Math.max(0, rule.maxRequests - currentCount);

        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.maxRequests));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        if (currentCount > rule.maxRequests) {
            log.warn("⚠️ Rate limit breached for client {} on endpoint {}. Count: {}, Limit: {}",
                    clientIp, uri, currentCount, rule.maxRequests);

            if (fintechMetricsService != null) {
                fintechMetricsService.recordRateLimitThrottled(uri, clientIp);
            }

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(rule.windowSeconds));

            String jsonResponse = "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"API rate limit exceeded for "
                    + rule.name + ". Maximum " + rule.maxRequests + " requests allowed per " + rule.windowSeconds + " seconds.\",\"path\":\""
                    + uri + "\"}";
            response.getWriter().write(jsonResponse);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolveRule(String uri, String method) {
        if ("POST".equalsIgnoreCase(method) && uri.equals("/api/auth/login")) {
            return new RateLimitRule("auth_login", 5, 60); // 5 per min
        }
        if ("POST".equalsIgnoreCase(method) && uri.startsWith("/api/auth/mfa/")) {
            return new RateLimitRule("auth_mfa", 3, 300); // 3 per 5 min
        }
        if ("POST".equalsIgnoreCase(method) && (uri.startsWith("/api/ledger/transfer") || uri.startsWith("/api/ledger/withdraw"))) {
            return new RateLimitRule("ledger_txn", 30, 60); // 30 per min
        }
        return null;
    }

    private long incrementAndGet(String key, int windowSeconds) {
        try {
            if (stringRedisTemplate != null) {
                Long count = stringRedisTemplate.opsForValue().increment(key);
                if (count != null && count == 1) {
                    stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
                }
                return count != null ? count : 1;
            }
        } catch (Exception e) {
            log.debug("Redis rate limiter unavailable, falling back to in-memory window: {}", e.getMessage());
        }

        // In-memory fallback
        long now = System.currentTimeMillis();
        Long expiry = localExpiries.get(key);
        if (expiry == null || now > expiry) {
            localExpiries.put(key, now + (windowSeconds * 1000L));
            localCounters.put(key, new AtomicInteger(1));
            return 1;
        }
        return localCounters.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private static class RateLimitRule {
        final String name;
        final int maxRequests;
        final int windowSeconds;

        RateLimitRule(String name, int maxRequests, int windowSeconds) {
            this.name = name;
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }
}
