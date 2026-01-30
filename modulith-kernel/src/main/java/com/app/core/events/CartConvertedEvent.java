package com.app.core.events;

import java.time.LocalDateTime;

/**
 * Published when a cart is converted to an order.
 * 
 * Publishers: modulith-checkout (CheckoutService)
 * Listeners: modulith-marketing (conversion tracking), modulith-analytics
 * (funnel analysis)
 * 
 * @param cartId      Cart identifier
 * @param orderId     Created order ID
 * @param userId      User who placed order
 * @param totalValue  Order total value
 * @param convertedAt When conversion happened
 */
public record CartConvertedEvent(
        String cartId,
        Long orderId,
        Long userId,
        double totalValue,
        LocalDateTime convertedAt) {
    public CartConvertedEvent(String cartId, Long orderId, Long userId, double totalValue) {
        this(cartId, orderId, userId, totalValue, LocalDateTime.now());
    }
}
