package com.app.cart.entities;

import com.app.governance.states.CartState;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class Cart {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long cartId;

	@jakarta.persistence.Version
	private Long version;

	@UpdateTimestamp
	private LocalDateTime lastUpdated;

	@jakarta.persistence.Column(name = "user_id", unique = true, nullable = true)
	private Long userId;

	@OneToMany(mappedBy = "cart", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, orphanRemoval = true)
	private List<CartItem> cartItems = new ArrayList<>();

	private BigDecimal totalPrice = BigDecimal.ZERO;

	private Long addressId;

	private String couponCode;

	@jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
	private CartState status = CartState.ACTIVE;

	public Cart() {
	}

	public Cart(Long cartId, Long userId, List<CartItem> cartItems, BigDecimal totalPrice) {
		this.cartId = cartId;
		this.userId = userId;
		this.cartItems = cartItems;
		this.totalPrice = totalPrice;
	}

	public Long getCartId() {
		return cartId;
	}

	public void setCartId(Long cartId) {
		this.cartId = cartId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public List<CartItem> getCartItems() {
		return cartItems;
	}

	public void setCartItems(List<CartItem> cartItems) {
		this.cartItems = cartItems;
	}

	public BigDecimal getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(BigDecimal totalPrice) {
		this.totalPrice = totalPrice;
	}

	public Long getAddressId() {
		return addressId;
	}

	public void setAddressId(Long addressId) {
		this.addressId = addressId;
	}

	public String getCouponCode() {
		return couponCode;
	}

	public void setCouponCode(String couponCode) {
		this.couponCode = couponCode;
	}

	public LocalDateTime getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(LocalDateTime lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public CartState getStatus() {
		return status;
	}

	public void setStatus(CartState status) {
		this.status = status;
	}
}
