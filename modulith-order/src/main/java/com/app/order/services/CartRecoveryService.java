package com.app.order.services;

import com.app.cart.entities.Cart;
import com.app.cart.repositories.CartRepo;
import com.app.governance.rules.RuleEngineService;
import com.app.governance.states.CartEvent;
import com.app.governance.states.OperationalStateMachineService;
import com.app.security.repositories.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Autonomous Abandoned Cart Recovery Service.
 * Detects idle carts and triggers the recovery lifecycle using State Machines
 * and SpEL rules.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class CartRecoveryService {

    private final CartRepo cartRepo;
    private final UserRepo userRepo;
    private final OperationalStateMachineService stateMachineService;
    private final RuleEngineService ruleEngine;

    @Scheduled(cron = "0 0 * * * ?") // Every hour
    @Transactional
    public void runRecovery() {
        log.info("Starting Autonomous Abandoned Cart Recovery Loop...");

        // 1-hour idle cutoff
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        List<Cart> idleCarts = cartRepo.findAbandonedCarts(cutoff);

        for (Cart cart : idleCarts) {
            processAbandonedCart(cart);
        }
    }

    private void processAbandonedCart(Cart cart) {
        if (cart.getUserId() == null) return;
        var user = userRepo.findById(cart.getUserId()).orElse(null);
        String email = (user != null) ? user.getEmail() : "unknown";

        Map<String, Object> context = new HashMap<>();
        context.put("cartValue", cart.getTotalPrice());
        context.put("itemCount", cart.getCartItems().size());

        // Dynamic SpEL Rule: Only recover carts with value > 500
        if (ruleEngine.evaluate("cartValue > 500", context)) {
            log.info("Nudging user {} for abandoned cart {} (Value: {})", email, cart.getCartId(),
                    cart.getTotalPrice());

            // Trigger State Machine
            stateMachineService.triggerCartEvent(cart.getCartId(), CartEvent.ABANDON);

            // Logic for sending email would go here (or be triggered by an event listener)
            log.info("Sent recovery notification to {}", email);
        }
    }
}
