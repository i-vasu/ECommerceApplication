package com.app.order.batch;

import com.app.cart.repositories.CartRepo;
import com.app.core.async.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Log4j2
@Component
@RequiredArgsConstructor
public class CartAbandonmentWorker {

    private final CartRepo cartRepo;
    private final com.app.security.repositories.UserRepo userRepo;
    private final EventProducer eventProducer;

    /**
     * Runs every hour to find carts abandoned for more than 24 hours.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void processAbandonedCarts() {
        log.info("Starting Cart Abandonment Worker...");

        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        var abandonedCarts = cartRepo.findAbandonedCarts(cutoff);

        log.info("Found {} abandoned carts.", abandonedCarts.size());

        for (var cart : abandonedCarts) {
            try {
                var user = userRepo.findById(cart.getUserId()).orElse(null);
                if (user != null) {
                    // Publish abandonment event for notification service
                    eventProducer.publish("cart_abandonment_events", user.getEmail());
                    log.info("Notified abandonment for cart of user: {}", user.getEmail());
                }
            } catch (Exception e) {
                log.error("Failed to process abandoned cart {}: {}", cart.getCartId(), e.getMessage());
            }
        }
    }
}
