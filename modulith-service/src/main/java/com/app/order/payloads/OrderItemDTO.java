package com.app.order.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.app.product.payloads.ProductDTO;

public class OrderItemDTO {

	private Long orderItemId;
	private ProductDTO product;
	private Integer quantity;
	private double discount;
	private double orderedProductPrice;

	public OrderItemDTO() {
	}

	public OrderItemDTO(Long orderItemId, ProductDTO product, Integer quantity, double discount,
			double orderedProductPrice) {
		this.orderItemId = orderItemId;
		this.product = product;
		this.quantity = quantity;
		this.discount = discount;
		this.orderedProductPrice = orderedProductPrice;
	}

	public Long getOrderItemId() {
		return orderItemId;
	}

	public void setOrderItemId(Long orderItemId) {
		this.orderItemId = orderItemId;
	}

	public ProductDTO getProduct() {
		return product;
	}

	public void setProduct(ProductDTO product) {
		this.product = product;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public double getDiscount() {
		return discount;
	}

	public void setDiscount(double discount) {
		this.discount = discount;
	}

	public double getOrderedProductPrice() {
		return orderedProductPrice;
	}

	public void setOrderedProductPrice(double orderedProductPrice) {
		this.orderedProductPrice = orderedProductPrice;
	}
}
