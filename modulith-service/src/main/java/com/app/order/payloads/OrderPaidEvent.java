package com.app.order.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

public record OrderPaidEvent(Long orderId, String email, Double amount, String pgPaymentId) implements Serializable {
}
