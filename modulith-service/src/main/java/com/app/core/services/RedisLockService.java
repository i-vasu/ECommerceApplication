package com.app.core.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class RedisLockService {

    private static final Logger log = LoggerFactory.getLogger(RedisLockService.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * Tries to acquire a lock with the given key and expiration time.
     * 
     * @param key      The lock key (e.g., "lock:erp-sync")
     * @param duration How long to hold the lock before auto-release
     * @return true if lock acquired, false otherwise
     */
    public boolean tryLock(String key, Duration duration) {
        log.trace("Attempting to acquire lock: {}", key);
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "LOCKED", duration);
        return Boolean.TRUE.equals(success);
    }

    /**
     * Releases the lock by deleting the key.
     * Note: In a robust implementation, we'd check if WE own the lock (via a unique
     * value),
     * but for a simple scheduler, delete is often sufficient if task duration <
     * lock duration.
     * 
     * @param key The lock key
     */
    public void unlock(String key) {
        log.trace("Unlocking: {}", key);
        redisTemplate.delete(key);
    }
}
