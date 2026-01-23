package com.app.product.events;

import com.app.core.events.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Listens to OrderCompletedEvent and maintains user purchase history in Redis.
 * This enables verified purchase badges on product reviews.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class PurchaseHistoryListener {

    private final StringRedisTemplate redisTemplate;

    private static final String PURCHASE_KEY_PREFIX = "user:purchases:";
    private static final long CACHE_TTL_DAYS = 365; // 1 year

    @ApplicationModuleListener
    public void onOrderCompleted(OrderCompletedEvent event) {
        log.info(">>> Product Module: Received OrderCompletedEvent for Order ID: {}", event.orderId());

        try {
            String userKey = PURCHASE_KEY_PREFIX + event.userEmail();

            // Add all product IDs from the order to user's purchase set
            event.productIds().forEach(productId -> {
                redisTemplate.opsForSet().add(userKey, productId.toString());
            });

            // Set expiration to prevent indefinite growth
            redisTemplate.expire(userKey, CACHE_TTL_DAYS, TimeUnit.DAYS);

            log.info(">>> Cached {} purchased products for user: {}",
                    event.productIds().size(), event.userEmail());

        } catch (Exception e) {
            log.error("Failed to cache purchase history for order: {}", event.orderId(), e);
            // Don't throw - this is a best-effort cache
        }
    }
}
