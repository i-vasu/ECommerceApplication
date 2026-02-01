package com.app.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class DistributedRateLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final String LUA_SCRIPT = "local key = KEYS[1] " +
            "local limit = tonumber(ARGV[1]) " +
            "local window = tonumber(ARGV[2]) " +
            "local current = redis.call('INCR', key) " +
            "if current == 1 then " +
            "  redis.call('EXPIRE', key, window) " +
            "end " +
            "if current > limit then " +
            "  return 0 " +
            "end " +
            "return 1";

    private final DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);

    /**
     * Tries to consume a token.
     * 
     * @param key           Identifer (IP/User/APIKey)
     * @param limit         Max requests
     * @param windowSeconds Window size in seconds
     * @return true if consumed, false if limited
     */
    public boolean tryConsume(String key, int limit, int windowSeconds) {
        String redisKey = "rate_limit:" + key;
        Long result = redisTemplate.execute(script, Collections.singletonList(redisKey), String.valueOf(limit),
                String.valueOf(windowSeconds));
        return result != null && result == 1L;
    }
}
