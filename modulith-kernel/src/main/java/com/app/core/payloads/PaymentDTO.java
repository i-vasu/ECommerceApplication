package com.app.core.payloads;

public record PaymentDTO(
		Long paymentId,
		String paymentMethod,
		String pgPaymentId,
		String pgOrderId,
		String pgSignature,
		String status) {
}
