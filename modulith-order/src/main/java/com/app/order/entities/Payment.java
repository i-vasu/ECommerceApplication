package com.app.order.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long paymentId;

	@OneToOne(mappedBy = "payment", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Order order;

	@NotBlank
	@Size(min = 4, message = "Payment method must contain atleast 4 characters")
	private String paymentMethod;

	private String pgPaymentId;
	private String pgOrderId;
	private String pgStatus;
	private String pgSignature;
	private Double walletAmount = 0.0;

	public Payment() {
	}

	public Payment(Long paymentId, Order order, String paymentMethod, String pgPaymentId, String pgOrderId,
			String pgStatus, String pgSignature) {
		this.paymentId = paymentId;
		this.order = order;
		this.paymentMethod = paymentMethod;
		this.pgPaymentId = pgPaymentId;
		this.pgOrderId = pgOrderId;
		this.pgStatus = pgStatus;
		this.pgSignature = pgSignature;
	}

	public Long getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(Long paymentId) {
		this.paymentId = paymentId;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
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

	public Double getWalletAmount() {
		return walletAmount;
	}

	public void setWalletAmount(Double walletAmount) {
		this.walletAmount = walletAmount;
	}
}
