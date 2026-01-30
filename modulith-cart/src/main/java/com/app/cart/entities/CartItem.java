package com.app.cart.entities;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_items")
public class CartItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long cartItemId;

	@ManyToOne
	@JoinColumn(name = "cart_id")
	private Cart cart;

	@jakarta.persistence.Column(name = "product_id")
	private Long productId;

	private String itemCode;

	private String productName;

	private Integer quantity;
	private java.math.BigDecimal discount;
	private java.math.BigDecimal productPrice;

	public CartItem() {
	}

	public CartItem(Long cartItemId, Cart cart, Long productId, String itemCode, String productName, Integer quantity,
			java.math.BigDecimal discount, java.math.BigDecimal productPrice) {
		this.cartItemId = cartItemId;
		this.cart = cart;
		this.productId = productId;
		this.itemCode = itemCode;
		this.productName = productName;
		this.quantity = quantity;
		this.discount = discount;
		this.productPrice = productPrice;
	}

	public Long getCartItemId() {
		return cartItemId;
	}

	public void setCartItemId(Long cartItemId) {
		this.cartItemId = cartItemId;
	}

	public Cart getCart() {
		return cart;
	}

	public void setCart(Cart cart) {
		this.cart = cart;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public String getItemCode() {
		return itemCode;
	}

	public void setItemCode(String itemCode) {
		this.itemCode = itemCode;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public java.math.BigDecimal getDiscount() {
		return discount;
	}

	public void setDiscount(java.math.BigDecimal discount) {
		this.discount = discount;
	}

	public java.math.BigDecimal getProductPrice() {
		return productPrice;
	}

	public void setProductPrice(java.math.BigDecimal productPrice) {
		this.productPrice = productPrice;
	}

}
