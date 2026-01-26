package com.app.marketplace.service;

import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, this::createNewBucket);
    }

    private Bucket createNewBucket(String key) {
        // Example logic: specific limits for Amazon vs Flipkart
        if ("amazon".equalsIgnoreCase(key)) {
            // 10 requests per second
            return Bucket.builder()
                    .addLimit(l -> l.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
                    .build();
        } else if ("flipkart".equalsIgnoreCase(key)) {
            // 5 requests per second
            return Bucket.builder()
                    .addLimit(l -> l.capacity(5).refillGreedy(5, Duration.ofSeconds(1)))
                    .build();
        }
        // Default: 2 requests per second
        return Bucket.builder()
                .addLimit(l -> l.capacity(2).refillGreedy(2, Duration.ofSeconds(1)))
                .build();
    }

    public boolean tryConsume(String key) {
        Bucket bucket = resolveBucket(key);
        return bucket.tryConsume(1);
    }
}
