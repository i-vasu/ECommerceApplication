package com.app.order.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.app.commerce.states.OrderStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.validation.constraints.Email;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long orderId;

	@Email
	@Column(nullable = false)
	private String email;

	@OneToMany(mappedBy = "order", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@BatchSize(size = 20)
	private List<OrderItem> orderItems = new ArrayList<>();

	private LocalDate orderDate;

	@OneToOne
	@JoinColumn(name = "payment_id")
	private Payment payment;

	@OneToOne
	@JoinColumn(name = "shipment_id")
	private Shipment shipment;

	private Double subTotal = 0.0;
	private Double totalTax = 0.0;
	private Double shippingCost = 0.0;
	private Double totalAmount;

	@Enumerated(EnumType.STRING)
	private OrderStatus orderStatus;

	private java.time.LocalDateTime deliveredDate;

	private String couponCode;

	private Double discountAmount = 0.0;

	private String erpNextOrderName;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@BatchSize(size = 20)
	private List<FulfillmentGroup> fulfillmentGroups = new ArrayList<>();

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@BatchSize(size = 20)
	private List<com.app.commerce.promotion.entities.OrderAdjustment> adjustments = new ArrayList<>();
}
