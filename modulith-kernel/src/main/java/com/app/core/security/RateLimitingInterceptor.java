package com.app.core.security;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        String apiKey = request.getHeader("X-API-KEY");
        String ip = request.getRemoteAddr();
        String identifier = (apiKey != null) ? apiKey : ip;

        String path = request.getRequestURI();
        boolean isHeavySide = path.contains("/search") || path.contains("/order") || path.contains("/checkout");

        // Key is Tenant : PathType : Identifier (IP/API-Key)
        String clientKey = String.format("%s:%s:%s", tenantId, isHeavySide ? "heavy" : "general", identifier);

        Bucket bucket = buckets.computeIfAbsent(clientKey, k -> {
            if (isHeavySide) {
                return RateLimitUtils.createBucket(10, 5); // 10 requests per minute for heavy ops
            } else {
                return RateLimitUtils.createBucket(100, 30); // 100 requests max, 30 per minute refill
            }
        });

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too many requests for your brand/IP. Please try again in " + waitForRefill + " seconds.");
            return false;
        }
    }
}
