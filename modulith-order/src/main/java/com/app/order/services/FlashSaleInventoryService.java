package com.app.order.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class FlashSaleInventoryService {

    private final StringRedisTemplate redisTemplate;

    private static final String FLASH_STOCK_KEY = "flash:stock:";

    // Lua script for atomic stock check-and-decrement in flash sale
    private static final String FLASH_RESERVE_SCRIPT = "local stock = tonumber(redis.call('get', KEYS[1])); " +
            "if stock == nil then return -1; end; " +
            "if stock >= tonumber(ARGV[1]) then " +
            "   return redis.call('decrby', KEYS[1], ARGV[1]); " +
            "else " +
            "   return -2; " +
            "end;";

    public void initializeFlashStock(Long productId, Integer quantity) {
        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        String key = "flash:" + tenantId + ":stock:" + productId;
        redisTemplate.opsForValue().set(key, String.valueOf(quantity), Duration.ofHours(48));
        log.info("Initialized Flash Stock for {}: {} [Tenant: {}]", productId, quantity, tenantId);
    }

    public boolean reserveFlashStock(Long productId, int quantity) {
        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(FLASH_RESERVE_SCRIPT, Long.class);
        String key = "flash:" + tenantId + ":stock:" + productId;

        Long result = redisTemplate.execute(script, Collections.singletonList(key), String.valueOf(quantity));

        if (result == -1) {
            log.error("Flash stock not initialized in Redis for product {} [Tenant: {}]", productId, tenantId);
            return false;
        } else if (result == -2) {
            log.warn("Flash stock sold out for product {} [Tenant: {}]", productId, tenantId);
            return false;
        } else {
            log.info("Flash stock reserved for {}. Remaining: {} [Tenant: {}]", productId, result, tenantId);
            return true;
        }
    }

    public void releaseFlashStock(Long productId, int quantity) {
        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        String key = "flash:" + tenantId + ":stock:" + productId;
        redisTemplate.opsForValue().increment(key, quantity);
        log.info("Released Flash Stock for {}: {} [Tenant: {}]", productId, quantity, tenantId);
    }

    public int getRemainingStock(Long productId) {
        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        String stock = redisTemplate.opsForValue().get("flash:" + tenantId + ":stock:" + productId);
        return stock != null ? Integer.parseInt(stock) : 0;
    }
}
