package com.app.order.services;

import com.app.cart.entities.Cart;
import com.app.cart.repositories.CartRepo;
import com.app.governance.states.CartState;
import com.app.governance.rules.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.app.core.events.CartAbandonedEvent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Autonomous Abandoned Cart Recovery Engine.
 * Automatically identifies cold carts and triggers recovery journeys via SpEL
 * rules.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class AbandonedCartRecoveryService {

    private final CartRepo cartRepo;
    private final RuleEngineService ruleEngine;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Runs every hour to find carts that haven't been updated for > 2 hours.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void runRecovery() {
        log.info("Starting Abandoned Cart Recovery scan...");

        LocalDateTime threshold = LocalDateTime.now().minusHours(2);
        List<Cart> abandonedCarts = cartRepo.findByStatusAndLastUpdatedBefore(CartState.ACTIVE, threshold);

        for (Cart cart : abandonedCarts) {
            processAbandonedCart(cart);
        }
    }

    private void processAbandonedCart(Cart cart) {
        if (cart.getUser() == null || cart.getCartItems().isEmpty()) {
            return;
        }

        Map<String, Object> context = new HashMap<>();
        context.put("total", cart.getTotalPrice());
        context.put("itemCount", cart.getCartItems().size());
        context.put("user", cart.getUser());

        // SpEL Rule: Recover if cart > $500 or has more than 3 items
        String recoveryRule = "total > 500 || itemCount >= 3";

        if (ruleEngine.evaluate(recoveryRule, context)) {
            log.info("Cart {} triggered recovery rule. Sending reminder...", cart.getCartId());

            // Emit Event instead of direct service call
            eventPublisher.publishEvent(new CartAbandonedEvent(
                    String.valueOf(cart.getCartId()),
                    cart.getUser() != null ? cart.getUser().getUserId() : 0L,
                    cart.getEmail(),
                    cart.getCartItems().stream()
                            .map(item -> new CartAbandonedEvent.CartItemData(
                                    item.getProductId() != null ? item.getProductId() : 0L,
                                    item.getItemCode(),
                                    item.getProductName(),
                                    item.getQuantity(),
                                    item.getProductPrice().doubleValue()))
                            .toList(),
                    cart.getTotalPrice().doubleValue()));

            cart.setStatus(CartState.ABANDONED);
            cartRepo.save(cart);
        }
    }
}
