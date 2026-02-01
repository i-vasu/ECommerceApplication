package com.app.order.listeners;

import com.app.cart.entities.Cart;
import com.app.cart.repositories.CartRepo;
import com.app.core.events.UserRegisteredEvent;
import com.app.security.repositories.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserActivityListener {

    private final CartRepo cartRepo;
    private final UserRepo userRepo;

    @ApplicationModuleListener
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("UserActivityListener: Initializing cart for newly registered user: {}", event.email());
        
        userRepo.findById(event.userId()).ifPresentOrElse(user -> {
            // Check if cart already exists (idempotency)
            if (cartRepo.findByUserId(user.getUserId()).isEmpty()) {
                Cart cart = new Cart();
                cart.setUserId(user.getUserId());
                cart.setTotalPrice(BigDecimal.ZERO);
                cartRepo.save(cart);
                log.info("Successfully initialized cart for user ID: {}", event.userId());
            } else {
                log.warn("Cart already exists for user ID: {}, skipping initialization", event.userId());
            }
        }, () -> log.error("User not found for ID: {}. Cannot initialize cart.", event.userId()));
    }
}
