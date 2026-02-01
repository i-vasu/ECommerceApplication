package com.app.finance.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@lombok.Getter
@lombok.Setter
@Entity
@Table(name = "payments")
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long paymentId;

	// DECOUPLED: Pointer to Order ID instead of JPA association
	private Long orderId;

	@NotBlank
	@Size(min = 4, message = "Payment method must contain atleast 4 characters")
	private String paymentMethod;

	private String pgPaymentId;
	private String pgOrderId;
	private String pgStatus;
	private String pgSignature;

	private BigDecimal walletAmount = BigDecimal.ZERO;

	private String status;
	private BigDecimal amount;

	public Payment() {
	}

	public Payment(Long paymentId, Long orderId, String paymentMethod, String pgPaymentId, String pgOrderId,
			String pgStatus, String pgSignature, BigDecimal walletAmount, String status, BigDecimal amount) {
		this.paymentId = paymentId;
		this.orderId = orderId;
		this.paymentMethod = paymentMethod;
		this.pgPaymentId = pgPaymentId;
		this.pgOrderId = pgOrderId;
		this.pgStatus = pgStatus;
		this.pgSignature = pgSignature;
		this.walletAmount = walletAmount;
		this.status = status;
		this.amount = amount;
	}

	public void markAsCaptured(String pgPaymentId) {
		this.pgPaymentId = pgPaymentId;
		this.pgStatus = "captured";
	}

	public Long getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(Long paymentId) {
		this.paymentId = paymentId;
	}

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
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

	public String getPgStatus() {
		return pgStatus;
	}

	public void setPgStatus(String pgStatus) {
		this.pgStatus = pgStatus;
	}

	public String getPgSignature() {
		return pgSignature;
	}

	public void setPgSignature(String pgSignature) {
		this.pgSignature = pgSignature;
	}

	public BigDecimal getWalletAmount() {
		return walletAmount;
	}

	public void setWalletAmount(BigDecimal walletAmount) {
		this.walletAmount = walletAmount;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
}
