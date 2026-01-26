package com.app.core.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;

public class RateLimitUtils {
    public static Bucket createBucket(int capacity, int tokensPerMinute) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(tokensPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
