package com.app.order.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class PaymentDTO {
	private Long paymentId;
	private String paymentMethod;

	// Payment gateway specific fields
	private String pgPaymentId; // Razorpay payment_id
	private String pgOrderId; // Razorpay order_id
	private String pgSignature; // Razorpay signature for verification
	private String status; // Payment status (PENDING, SUCCESS, FAILED)

	public PaymentDTO() {
	}

	public PaymentDTO(Long paymentId, String paymentMethod, String pgPaymentId, String pgOrderId, String pgSignature,
			String status) {
		this.paymentId = paymentId;
		this.paymentMethod = paymentMethod;
		this.pgPaymentId = pgPaymentId;
		this.pgOrderId = pgOrderId;
		this.pgSignature = pgSignature;
		this.status = status;
	}

	public Long getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(Long paymentId) {
		this.paymentId = paymentId;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getPgPaymentId() {
		return pgPaymentId;
	}

	public void setPgPaymentId(String pgPaymentId) {
		this.pgPaymentId = pgPaymentId;
	}

	public String getPgOrderId() {
		return pgOrderId;
	}

	public void setPgOrderId(String pgOrderId) {
		this.pgOrderId = pgOrderId;
	}

	public String getPgSignature() {
		return pgSignature;
	}

	public void setPgSignature(String pgSignature) {
		this.pgSignature = pgSignature;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
