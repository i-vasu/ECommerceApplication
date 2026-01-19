package com.app.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.app.core.APIException;
import java.time.Duration;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
public class InventoryReservationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InventoryReservationService.class);

    private final StringRedisTemplate redisTemplate;

    private static final String INVENTORY_KEY_PREFIX = "inventory:stock:";

    // Lua script for atomic stock check-and-decrement
    private static final String RESERVE_SCRIPT = "local stock = tonumber(redis.call('get', KEYS[1])); " +
            "if stock == nil then return -1; end; " +
            "if stock >= tonumber(ARGV[1]) then " +
            "   return redis.call('decrby', KEYS[1], ARGV[1]); " +
            "else " +
            "   return -2; " +
            "end;";

    // 1. Initialize Stock in Redis (Cache Warming)
    public void setStock(String itemCode, int quantity) {
        redisTemplate.opsForValue().set(INVENTORY_KEY_PREFIX + itemCode, String.valueOf(quantity),
                Duration.ofHours(24));
    }

    // 2. Atomic Reservation
    public boolean reserveStock(String itemCode, int quantity) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RESERVE_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(INVENTORY_KEY_PREFIX + itemCode),
                String.valueOf(quantity));

        if (result == -1) {
            log.warn("Stock not initialized in Redis for {}, falling back to ERPNext (slow)", itemCode);
            return false; // Indicating caller should fetch from source
        } else if (result == -2) {
            log.info("Insufficient stock for reservation: {}", itemCode);
            throw new APIException("Item out of stock: " + itemCode);
        } else {
            log.info("Stock reserved for {}. Remaining: {}", itemCode, result);
            return true;
        }
    }

    // 3. Rollback (if payment fails)
    public void releaseStock(String itemCode, int quantity) {
        redisTemplate.opsForValue().increment(INVENTORY_KEY_PREFIX + itemCode, quantity);
        log.info("Stock released for {}", itemCode);
    }
}
