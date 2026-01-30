package com.app.core.events;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Published when a cart is abandoned (user leaves without checkout).
 * 
 * Publishers: modulith-cart (CartService)
 * Listeners: modulith-marketing (sends reminder emails), modulith-analytics
 * (tracks abandonment)
 * 
 * @param cartId      Cart identifier
 * @param userId      User who abandoned cart
 * @param email       User email for reminders
 * @param items       List of items in abandoned cart
 * @param totalValue  Total cart value
 * @param abandonedAt When cart was abandoned
 */
public record CartAbandonedEvent(
        String cartId,
        Long userId,
        String email,
        List<CartItemData> items,
        double totalValue,
        LocalDateTime abandonedAt) {
    public CartAbandonedEvent(String cartId, Long userId, String email, List<CartItemData> items, double totalValue) {
        this(cartId, userId, email, items, totalValue, LocalDateTime.now());
    }

    public record CartItemData(
            Long productId,
            String itemCode,
            String productName,
            int quantity,
            double price) {
    }
}
