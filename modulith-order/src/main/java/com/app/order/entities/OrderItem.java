package com.app.order.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType; // Added for FetchType

// Removed Lombok imports:
// import lombok.AllArgsConstructor;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// Assuming Product and ProductVariant are in the same package or need to be imported
import com.app.catalog.entities.Product;
import com.app.catalog.entities.ProductVariant;

@Entity
// Removed Lombok annotations: @Data, @AllArgsConstructor, @NoArgsConstructor
@Table(name = "order_items")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long orderItemId;

	@ManyToOne(fetch = FetchType.LAZY) // Modified with FetchType
	@JoinColumn(name = "order_id")
	private Order order;

	@ManyToOne
	@JoinColumn(name = "product_id")
	private Product product; // New field

	@ManyToOne
	@JoinColumn(name = "variant_id")
	private ProductVariant variant; // New field

	private Integer quantity;
	private java.math.BigDecimal orderedPrice;
	private java.math.BigDecimal discount; // Added back

	private String productName;
	private String itemCode;

	private String status; // NORMAL, RETURN_REQUESTED, RETURNED, REJECTED
	private Integer returnedQuantity = 0;

	@jakarta.persistence.OneToMany(mappedBy = "orderItem", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
	private java.util.List<OrderItemTaxDetail> taxDetails = new java.util.ArrayList<>();

	// Manual No-argument constructor
	public OrderItem() {
	}

	// Manual All-argument constructor
	public OrderItem(Long orderItemId, Order order, Product product, ProductVariant variant, Integer quantity,
			java.math.BigDecimal orderedPrice, String productName, String itemCode) {
		this.orderItemId = orderItemId;
		this.order = order;
		this.product = product;
		this.variant = variant;
		this.quantity = quantity;
		this.orderedPrice = orderedPrice;
		this.productName = productName;
		this.itemCode = itemCode;
	}

	// Manual Getters and Setters
	public Long getOrderItemId() {
		return orderItemId;
	}

	public void setOrderItemId(Long orderItemId) {
		this.orderItemId = orderItemId;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public ProductVariant getVariant() {
		return variant;
	}

	public void setVariant(ProductVariant variant) {
		this.variant = variant;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public java.math.BigDecimal getOrderedPrice() {
		return orderedPrice;
	}

	public void setOrderedPrice(java.math.BigDecimal orderedPrice) {
		this.orderedPrice = orderedPrice;
	}

	public java.math.BigDecimal getDiscount() {
		return discount;
	}

	public void setDiscount(java.math.BigDecimal discount) {
		this.discount = discount;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getItemCode() {
		return itemCode;
	}

	public void setItemCode(String itemCode) {
		this.itemCode = itemCode;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getReturnedQuantity() {
		return returnedQuantity;
	}

	public void setReturnedQuantity(Integer returnedQuantity) {
		this.returnedQuantity = returnedQuantity;
	}

	public java.util.List<OrderItemTaxDetail> getTaxDetails() {
		return taxDetails;
	}

	public void setTaxDetails(java.util.List<OrderItemTaxDetail> taxDetails) {
		this.taxDetails = taxDetails;
	}
}
