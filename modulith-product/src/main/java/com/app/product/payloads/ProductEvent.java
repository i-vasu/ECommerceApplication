package com.app.product.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public record ProductEvent(Long productId, String eventType) {
}
