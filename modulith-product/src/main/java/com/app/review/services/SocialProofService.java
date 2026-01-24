package com.app.review.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class SocialProofService {

    private final StringRedisTemplate redisTemplate;

    // Keys format: product:view_count:{productId}
    // Keys format: product:recent_purchase:{productId}

    public void incrementViewCount(Long productId) {
        String key = "product:view_count:" + productId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofHours(24));
    }

    public String getViewCount(Long productId) {
        return redisTemplate.opsForValue().get("product:view_count:" + productId);
    }

    public void recordRecentPurchase(Long productId, String location) {
        String key = "product:recent_purchase:" + productId;
        redisTemplate.opsForValue().set(key, location, Duration.ofHours(6));
    }

    public Map<String, String> getSocialPulse(Long productId) {
        Map<String, String> pulse = new HashMap<>();
        pulse.put("views", getViewCount(productId));
        pulse.put("recentLocation", redisTemplate.opsForValue().get("product:recent_purchase:" + productId));
        return pulse;
    }
}
