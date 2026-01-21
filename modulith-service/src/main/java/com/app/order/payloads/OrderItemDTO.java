package com.app.order.payloads;

import com.app.product.payloads.ProductDTO;

public record OrderItemDTO(
		Long orderItemId,
		ProductDTO product,
		Integer quantity,
		double discount,
		double orderedProductPrice) {
}
