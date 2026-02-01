package com.app.core.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final DistributedRateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        String apiKey = request.getHeader("X-API-KEY");
        String ip = getClientIP(request);
        String identifier = (apiKey != null) ? apiKey : ip;

        String path = request.getRequestURI();
        boolean isHeavySide = path.contains("/search") || path.contains("/order") || path.contains("/checkout");

        // Key is Tenant : PathType : Identifier (IP/API-Key)
        String clientKey = String.format("%s:%s:%s", tenantId, isHeavySide ? "heavy" : "general", identifier);

        int limit = isHeavySide ? 10 : 100;
        int window = 60; // 1 minute

        boolean allowed = rateLimiter.tryConsume(clientKey, limit, window);

        if (allowed) {
            return true;
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests. Please try again in a minute.");
            return false;
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        return (xfHeader == null) ? request.getRemoteAddr() : xfHeader.split(",")[0];
    }
}
