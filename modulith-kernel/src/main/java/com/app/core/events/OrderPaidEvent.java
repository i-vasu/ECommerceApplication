package com.app.core.events;

import java.io.Serializable;

public record OrderPaidEvent(Long orderId, String email, Double amount, String pgPaymentId) implements Serializable {
}
