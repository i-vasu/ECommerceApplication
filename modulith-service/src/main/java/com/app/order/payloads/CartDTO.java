package com.app.order.payloads;

import java.util.ArrayList;
import java.util.List;

import com.app.product.payloads.ProductDTO;

public class CartDTO {

	private Long cartId;
	private Double totalPrice = 0.0;
	private List<ProductDTO> products = new ArrayList<>();

	public CartDTO() {
	}

	public CartDTO(Long cartId, Double totalPrice, List<ProductDTO> products) {
		this.cartId = cartId;
		this.totalPrice = totalPrice;
		this.products = products;
	}

	public Long getCartId() {
		return cartId;
	}

	public void setCartId(Long cartId) {
		this.cartId = cartId;
	}

	public Double getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(Double totalPrice) {
		this.totalPrice = totalPrice;
	}

	public List<ProductDTO> getProducts() {
		return products;
	}

	public void setProducts(List<ProductDTO> products) {
		this.products = products;
	}
}
