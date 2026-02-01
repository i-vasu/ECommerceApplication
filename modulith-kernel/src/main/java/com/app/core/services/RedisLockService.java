package com.app.core.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisLockService {

    private static final Logger log = LoggerFactory.getLogger(RedisLockService.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * Tries to acquire a lock with the given key and expiration time.
     * Automatically prefixes the key with the current tenantId for isolation.
     * 
     * @param key      The lock key (e.g., "erp-sync")
     * @param duration How long to hold the lock before auto-release
     * @return true if lock acquired, false otherwise
     */
    public boolean tryLock(String key, Duration duration) {
        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        String fullKey = "lock:" + tenantId + ":" + key;

        log.trace("Attempting to acquire lock: {}", fullKey);
        Boolean success = redisTemplate.opsForValue().setIfAbsent(fullKey, "LOCKED", duration);
        return Boolean.TRUE.equals(success);
    }

    /**
     * Releases the lock by deleting the tenant-prefixed key.
     * 
     * @param key The lock key
     */
    public void unlock(String key) {
        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        String fullKey = "lock:" + tenantId + ":" + key;

        log.trace("Unlocking: {}", fullKey);
        redisTemplate.delete(fullKey);
    }
}
