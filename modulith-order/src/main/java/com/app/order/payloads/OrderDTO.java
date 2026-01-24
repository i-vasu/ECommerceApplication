package com.app.order.payloads;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record OrderDTO(
		Long orderId,
		String email,
		List<OrderItemDTO> orderItems,
		LocalDate orderDate,
		PaymentDTO payment,
		Double totalAmount,
		String orderStatus,
		String marketplaceOrderId,
		String customerEmail,
		String source,
		String couponCode,
		Double discountAmount) {
	public OrderDTO(Long orderId, String email, List<OrderItemDTO> orderItems, LocalDate orderDate, PaymentDTO payment,
			Double totalAmount, String orderStatus) {
		this(orderId, email, orderItems, orderDate, payment, totalAmount, orderStatus, null, null, null, null, null);
	}
}
