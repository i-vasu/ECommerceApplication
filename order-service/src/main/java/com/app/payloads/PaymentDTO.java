package com.app.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
	private Long paymentId;
	private String paymentMethod;

	// Payment gateway specific fields
	private String pgPaymentId; // Razorpay payment_id
	private String pgOrderId; // Razorpay order_id
	private String pgSignature; // Razorpay signature for verification
	private String status; // Payment status (PENDING, SUCCESS, FAILED)

}
