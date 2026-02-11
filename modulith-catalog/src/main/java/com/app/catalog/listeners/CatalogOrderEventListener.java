package com.app.catalog.listeners;

import com.app.core.events.OrderCompletedEvent;
import com.app.core.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Listens for Order completion to update catalog-specific metadata
 * such as verified purchase history for reviews.
 */
@Component
@RequiredArgsConstructor
public class CatalogOrderEventListener {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CatalogOrderEventListener.class);

    private final StringRedisTemplate redisTemplate;

    @ApplicationModuleListener
    public void onOrderCompleted(OrderCompletedEvent event) {
        log.info("Catalog Module: Tracking purchase history for User {} and Order {}", event.userId(), event.orderId());
        
        String tenantId = TenantContext.getTenantId();
        // Set: user:{tenant}:purchases:{email} -> Set of Product IDs
        String key = String.format("user:%s:purchases:%s", tenantId, event.userEmail());
        
        for (Long productId : event.productIds()) {
            redisTemplate.opsForSet().add(key, String.valueOf(productId));
        }
        
        // Expire after 1 year of inactivity
        redisTemplate.expire(key, Duration.ofDays(365));
        
        // Incremental Best Seller tracking (Simplified)
        for (Long productId : event.productIds()) {
            String bestSellerKey = String.format("catalog:%s:bestsellers", tenantId);
            redisTemplate.opsForZSet().incrementScore(bestSellerKey, String.valueOf(productId), 1.0);
        }
    }
}
