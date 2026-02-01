package com.app.order.payloads;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record OrderDTO(
		Long orderId,
		String email,
		List<OrderItemDTO> orderItems,
		LocalDate orderDate,
		PaymentDTO payment,
		java.math.BigDecimal totalAmount,
		String orderStatus,
		String marketplaceOrderId,
		String customerEmail,
		String source,
		String couponCode,
		java.math.BigDecimal discountAmount) {
	public OrderDTO(Long orderId, String email, List<OrderItemDTO> orderItems, LocalDate orderDate, PaymentDTO payment,
			java.math.BigDecimal totalAmount, String orderStatus) {
		this(orderId, email, orderItems, orderDate, payment, totalAmount, orderStatus, null, null, null, null, null);
	}
}
