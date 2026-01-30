package com.app.order.payloads;

import com.app.catalog.payloads.ProductDTO;

public record OrderItemDTO(
		Long orderItemId,
		ProductDTO product,
		Integer quantity,
		java.math.BigDecimal discount,
		java.math.BigDecimal orderedProductPrice) {
}
