package com.app.core.events;

import java.math.BigDecimal;
import java.io.Serializable;

/**
 * Domain event published when payment is successfully captured for an order.
 */
public record OrderPaidEvent(
    Long orderId, 
    String email, 
    BigDecimal amount, 
    String pgPaymentId,
    String paymentMethod
) implements Serializable {}
