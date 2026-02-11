package com.app.core.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Interceptor using Token Bucket algorithm.
 * Protects API endpoints from abuse and DDoS attacks.
 * 
 * Limits:
 * - 100 requests per minute per IP for general endpoints
 * - 10 requests per minute for sensitive endpoints (login, payment)
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = getClientKey(request);
        Bucket bucket = resolveBucket(key, request.getRequestURI());

        if (bucket.tryConsume(1)) {
            return true;
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
            response.setContentType("application/json");
            return false;
        }
    }

    private Bucket resolveBucket(String key, String uri) {
        return cache.computeIfAbsent(key, k -> createNewBucket(uri));
    }

    private Bucket createNewBucket(String uri) {
        Bandwidth limit;
        
        // Sensitive endpoints get stricter limits
        if (isSensitiveEndpoint(uri)) {
            limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
        } else {
            limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        }
        
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private boolean isSensitiveEndpoint(String uri) {
        return uri.contains("/api/v1/auth/login") ||
               uri.contains("/api/v1/auth/register") ||
               uri.contains("/api/v1/payment") ||
               uri.contains("/api/v1/checkout/place-order");
    }

    private String getClientKey(HttpServletRequest request) {
        String clientIp = getClientIP(request);
        return "rate_limit:" + clientIp;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
