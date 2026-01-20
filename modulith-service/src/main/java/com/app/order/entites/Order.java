package com.app.order.entites;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
	private List<OrderItem> orderItems = new ArrayList<>();

	private LocalDate orderDate;

	@OneToOne
	@JoinColumn(name = "payment_id")
	private Payment payment;

	@OneToOne
	@JoinColumn(name = "shipment_id")
	private Shipment shipment;

	private Double totalAmount;
    
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
	private com.app.commerce.states.OrderStatus orderStatus;
    
	private String erpNextOrderName;

	public Order() {
	}

	public Order(Long orderId, String email, List<OrderItem> orderItems, LocalDate orderDate, Payment payment,
			Shipment shipment, Double totalAmount, com.app.commerce.states.OrderStatus orderStatus, String erpNextOrderName) {
		this.orderId = orderId;
		this.email = email;
		this.orderItems = orderItems;
		this.orderDate = orderDate;
		this.payment = payment;
		this.shipment = shipment;
		this.totalAmount = totalAmount;
		this.orderStatus = orderStatus;
		this.erpNextOrderName = erpNextOrderName;
	}

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public List<OrderItem> getOrderItems() {
		return orderItems;
	}

	public void setOrderItems(List<OrderItem> orderItems) {
		this.orderItems = orderItems;
	}

	public LocalDate getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(LocalDate orderDate) {
		this.orderDate = orderDate;
	}

	public Payment getPayment() {
		return payment;
	}

	public void setPayment(Payment payment) {
		this.payment = payment;
	}

	public Shipment getShipment() {
		return shipment;
	}

	public void setShipment(Shipment shipment) {
		this.shipment = shipment;
	}

	public Double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(Double totalAmount) {
		this.totalAmount = totalAmount;
	}

	public com.app.commerce.states.OrderStatus getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(com.app.commerce.states.OrderStatus orderStatus) {
		this.orderStatus = orderStatus;
	}

	public String getErpNextOrderName() {
		return erpNextOrderName;
	}

	public void setErpNextOrderName(String erpNextOrderName) {
		this.erpNextOrderName = erpNextOrderName;
	}
}
